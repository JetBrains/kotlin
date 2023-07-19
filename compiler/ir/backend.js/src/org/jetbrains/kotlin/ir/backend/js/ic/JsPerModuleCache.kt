/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import java.io.File

class JsPerModuleCache(private val moduleArtifacts: List<ModuleArtifact>) : JsMultiArtifactCache<JsPerModuleCache.CachedModuleInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_MODULE_JS = "module.js"
        private const val CACHED_MODULE_JS_MAP = "module.js.map"
        private const val CACHED_MODULE_D_TS = "module.d.ts"
    }

    class CachedModuleInfo(
        val artifact: ModuleArtifact,
        override val jsIrHeader: JsIrModuleHeader,
        var crossModuleReferencesHash: ICHash = ICHash()
    ) : CacheInfo

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedModuleInfo>()

    private fun ModuleArtifact.fetchModuleInfo() = File(artifactsDir, JS_MODULE_HEADER).useCodedInputIfExists {
        val crossModuleReferencesHash = ICHash.fromProtoStream(this)
        val reexportedInModuleWithName = ifTrue { readString() }
        val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

        CachedModuleInfo(
            artifact = this@fetchModuleInfo,
            jsIrHeader = JsIrModuleHeader(
                moduleName = moduleSafeName,
                externalModuleName = moduleExternalName,
                definitions = definitions,
                nameBindings = nameBindings,
                optionalCrossModuleImports = optionalCrossModuleImports,
                reexportedInModuleWithName = reexportedInModuleWithName,
                associatedModule = null
            ),
            crossModuleReferencesHash = crossModuleReferencesHash
        )
    }

    private fun CachedModuleInfo.commitModuleInfo() = artifact.artifactsDir?.let { cacheDir ->
        File(cacheDir, JS_MODULE_HEADER).useCodedOutput {
            crossModuleReferencesHash.toProtoStream(this)
            ifNotNull(jsIrHeader.reexportedInModuleWithName) { writeStringNoTag(it) }
            commitJsIrModuleHeaderNames(jsIrHeader)
        }
    }

    override fun loadJsIrModule(cacheInfo: CachedModuleInfo) = cacheInfo.artifact.loadJsIrModule()

    override fun getMainModuleAndDependencies(cacheInfo: List<CachedModuleInfo>) =
        cacheInfo.last() to cacheInfo.dropLast(1)

    override fun fetchCompiledJsCodeForNullCacheInfo() =
        error("Should never happen for per module granularity")

    override fun fetchCompiledJsCode(cacheInfo: CachedModuleInfo) = cacheInfo.artifact.artifactsDir?.let { cacheDir ->
        val jsCodeFile = File(cacheDir, CACHED_MODULE_JS).ifExists { this }
        val sourceMapFile = File(cacheDir, CACHED_MODULE_JS_MAP).ifExists { this }
        val tsDefinitionsFile = File(cacheDir, CACHED_MODULE_D_TS).ifExists { this }
        jsCodeFile?.let { CompilationOutputsCached(it, sourceMapFile, tsDefinitionsFile) }
    }

    override fun commitCompiledJsCode(cacheInfo: CachedModuleInfo, compilationOutputs: CompilationOutputsBuilt): CompilationOutputs =
        cacheInfo.artifact.artifactsDir?.let { cacheDir ->
            val jsCodeFile = File(cacheDir, CACHED_MODULE_JS)
            val jsMapFile = File(cacheDir, CACHED_MODULE_JS_MAP)
            File(cacheDir, CACHED_MODULE_D_TS).writeIfNotNull(compilationOutputs.tsDefinitions?.raw)
            compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
        } ?: compilationOutputs

    override fun loadProgramHeadersFromCache(): List<CachedModuleInfo> {
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

    override fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>) {
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
