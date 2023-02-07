/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import java.io.File

class JsMultiModuleCache(private val moduleArtifacts: List<ModuleArtifact>) {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_MODULE_JS = "module.js"
        private const val CACHED_MODULE_JS_MAP = "module.js.map"
        private const val CACHED_MODULE_D_TS = "module.d.ts"
    }

    private enum class NameType(val typeMask: Int) {
        DEFINITIONS(0b1), NAME_BINDINGS(0b10), OPTIONAL_IMPORTS(0b100)
    }

    class CachedModuleInfo(val artifact: ModuleArtifact, val jsIrHeader: JsIrModuleHeader, var crossModuleReferencesHash: ICHash = ICHash())

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedModuleInfo>()

    private fun ModuleArtifact.fetchModuleInfo() = File(artifactsDir, JS_MODULE_HEADER).useCodedInputIfExists {
        val definitions = mutableSetOf<String>()
        val nameBindings = mutableMapOf<String, String>()
        val optionalCrossModuleImports = hashSetOf<String>()

        val crossModuleReferencesHash = ICHash.fromProtoStream(this)
        val hasJsExports = readBool()
        repeat(readInt32()) {
            val tag = readString()
            val mask = readInt32()
            if (mask and NameType.DEFINITIONS.typeMask != 0) {
                definitions += tag
            }
            if (mask and NameType.OPTIONAL_IMPORTS.typeMask != 0) {
                optionalCrossModuleImports += tag
            }
            if (mask and NameType.NAME_BINDINGS.typeMask != 0) {
                nameBindings[tag] = readString()
            }
        }
        CachedModuleInfo(
            artifact = this@fetchModuleInfo,
            jsIrHeader = JsIrModuleHeader(
                moduleName = moduleSafeName,
                externalModuleName = moduleExternalName,
                definitions = definitions,
                nameBindings = nameBindings,
                optionalCrossModuleImports = optionalCrossModuleImports,
                hasJsExports = hasJsExports,
                associatedModule = null
            ),
            crossModuleReferencesHash = crossModuleReferencesHash
        )
    }

    private fun CachedModuleInfo.commitModuleInfo() = artifact.artifactsDir?.let { cacheDir ->
        File(cacheDir, JS_MODULE_HEADER).useCodedOutput {
            val names = mutableMapOf<String, Pair<Int, String?>>()
            for ((tag, name) in jsIrHeader.nameBindings) {
                names[tag] = NameType.NAME_BINDINGS.typeMask to name
            }
            for (tag in jsIrHeader.optionalCrossModuleImports) {
                val maskAndName = names[tag]
                names[tag] = ((maskAndName?.first ?: 0) or NameType.OPTIONAL_IMPORTS.typeMask) to maskAndName?.second
            }
            for (tag in jsIrHeader.definitions) {
                val maskAndName = names[tag]
                names[tag] = ((maskAndName?.first ?: 0) or NameType.DEFINITIONS.typeMask) to maskAndName?.second
            }
            crossModuleReferencesHash.toProtoStream(this)
            writeBoolNoTag(jsIrHeader.hasJsExports)
            writeInt32NoTag(names.size)
            for ((tag, maskAndName) in names) {
                writeStringNoTag(tag)
                writeInt32NoTag(maskAndName.first)
                if (maskAndName.second != null) {
                    writeStringNoTag(maskAndName.second)
                }
            }
        }
    }

    private fun File.writeIfNotNull(data: String?) {
        if (data != null) {
            parentFile?.mkdirs()
            writeText(data)
        } else {
            delete()
        }
    }

    fun fetchCompiledJsCode(artifact: ModuleArtifact) = artifact.artifactsDir?.let { cacheDir ->
        val jsCodeFile = File(cacheDir, CACHED_MODULE_JS).ifExists { this }
        val sourceMapFile = File(cacheDir, CACHED_MODULE_JS_MAP).ifExists { this }
        val tsDefinitionsFile = File(cacheDir, CACHED_MODULE_D_TS).ifExists { this }
        jsCodeFile?.let { CompilationOutputsCached(it, sourceMapFile, tsDefinitionsFile) }
    }

    fun commitCompiledJsCode(artifact: ModuleArtifact, compilationOutputs: CompilationOutputsBuilt): CompilationOutputs =
        artifact.artifactsDir?.let { cacheDir ->
            val jsCodeFile = File(cacheDir, CACHED_MODULE_JS)
            val jsMapFile = File(cacheDir, CACHED_MODULE_JS_MAP)
            File(cacheDir, CACHED_MODULE_D_TS).writeIfNotNull(compilationOutputs.tsDefinitions?.raw)
            compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
        } ?: compilationOutputs

    fun loadProgramHeadersFromCache(): List<CachedModuleInfo> {
        return moduleArtifacts.map { artifact ->
            fun loadModuleInfo() = CachedModuleInfo(artifact, artifact.loadJsIrModule().makeModuleHeader())
            val actualInfo = when {
                artifact.forceRebuildJs -> loadModuleInfo()
                artifact.fileArtifacts.any { it.isModified() } -> loadModuleInfo()
                else -> artifact.fetchModuleInfo() ?: loadModuleInfo()
            }
            headerToCachedInfo[actualInfo.jsIrHeader] = actualInfo
            actualInfo
        }
    }

    fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>) {
        for ((header, references) in crossModuleReferences) {
            val cachedInfo = headerToCachedInfo[header] ?: notFoundIcError("artifact for module ${header.moduleName}")
            val actualCrossModuleHash = references.crossModuleReferencesHashForIC()
            if (header.associatedModule == null && cachedInfo.crossModuleReferencesHash != actualCrossModuleHash) {
                header.associatedModule = cachedInfo.artifact.loadJsIrModule()
            }
            header.associatedModule?.let {
                cachedInfo.crossModuleReferencesHash = actualCrossModuleHash
                cachedInfo.commitModuleInfo()
            }
        }
    }
}
