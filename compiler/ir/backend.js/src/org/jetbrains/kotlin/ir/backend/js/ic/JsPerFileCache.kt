/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class JsPerFileCache(private val moduleArtifacts: List<ModuleArtifact>) : JsMultiArtifactCache<JsPerFileCache.CachedFileInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_FILE_JS = "file.js"
        private const val CACHED_EXPORT_FILE_JS = "file.export.js"
        private const val CACHED_FILE_JS_MAP = "file.js.map"
        private const val CACHED_FILE_D_TS = "file.d.ts"
    }

    class CachedFileInfo(
        val moduleArtifact: ModuleArtifact,
        val fileArtifact: SrcFileArtifact,
        val isExportFileCachedInfo: Boolean = false,
    ) : CacheInfo {
        var dtsHash: Long? = null
        var crossFileReferencesHash: ICHash = ICHash()
        var exportFileCachedInfo: CachedFileInfo? = null
        override lateinit var jsIrHeader: JsIrModuleHeader

        constructor(
            jsIrModuleHeader: JsIrModuleHeader,
            moduleArtifact: ModuleArtifact,
            fileArtifact: SrcFileArtifact,
            isExportFileCachedInfo: Boolean = false,
            tsDeclarationsHash: Long? = null,
        ) : this(moduleArtifact, fileArtifact, isExportFileCachedInfo) {
            jsIrHeader = jsIrModuleHeader
            dtsHash = tsDeclarationsHash
        }

        val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }

        val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(if (isExportFileCachedInfo) CACHED_EXPORT_FILE_JS else CACHED_FILE_JS) }
        val dtsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { runIf(isExportFileCachedInfo) { getArtifactWithName(CACHED_FILE_D_TS) } }
        val sourceMapFileArtifact by lazy(LazyThreadSafetyMode.NONE) { runIf(!isExportFileCachedInfo) { getArtifactWithName(CACHED_FILE_JS_MAP) } }

        private fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "$filePrefix.$name") }

        private val filePrefix by lazy(LazyThreadSafetyMode.NONE) {
            val pathHash = fileArtifact.srcFilePath.cityHash64String()
            "${fileArtifact.srcFilePath.substringAfterLast('/')}.$pathHash"
        }
    }

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedFileInfo>()
    private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

    private fun JsIrProgramFragment.getMainFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameFor(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.getExportFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameForExporterFile(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.asIrModuleHeader(moduleName: String): JsIrModuleHeader {
        return JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings.mapValues { v -> v.value.toString() },
            optionalCrossModuleImports = optionalCrossModuleImports,
            associatedModule = null
        )
    }

    private fun SrcFileArtifact.loadJsIrModuleHeaders(moduleArtifact: ModuleArtifact) = with(loadJsIrFragments()) {
        LoadedJsIrModuleHeaders(
            mainFragment.run { asIrModuleHeader(getMainFragmentExternalName(moduleArtifact)) },
            exportFragment?.run { asIrModuleHeader(mainFragment.getExportFragmentExternalName(moduleArtifact)) },
        )
    }

    private fun CodedInputStream.loadSingleCachedFileInfo(cachedFileInfo: CachedFileInfo) = cachedFileInfo.also {
        val moduleName = readString()

        it.crossFileReferencesHash = ICHash.fromProtoStream(this)
        it.dtsHash = runIf(readBool()) { readInt64() }

        val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

        it.jsIrHeader = JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings,
            optionalCrossModuleImports = optionalCrossModuleImports,
            associatedModule = null
        )
    }

    private fun <T> CachedFileInfo.readModuleHeaderCache(f: CodedInputStream.() -> T): T? = moduleHeaderArtifact?.useCodedInputIfExists(f)

    private fun ModuleArtifact.fetchFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo>? {
        val moduleArtifact = this
        val mainFileCachedFileInfo = CachedFileInfo(moduleArtifact, fileArtifact)

        return mainFileCachedFileInfo.readModuleHeaderCache {
            mainFileCachedFileInfo.run {
                exportFileCachedInfo = fetchFileInfoForExportedPart(this)
                loadSingleCachedFileInfo(this)
                listOfNotNull(exportFileCachedInfo, this)
            }
        }
    }

    private fun CodedInputStream.fetchFileInfoForExportedPart(mainCachedFileInfo: CachedFileInfo): CachedFileInfo? {
        return ifTrue {
            loadSingleCachedFileInfo(
                CachedFileInfo(
                    mainCachedFileInfo.moduleArtifact,
                    mainCachedFileInfo.fileArtifact,
                    isExportFileCachedInfo = true
                )
            )
        }
    }

    private fun CodedOutputStream.commitSingleFileInfo(cachedFileInfo: CachedFileInfo) {
        writeStringNoTag(cachedFileInfo.jsIrHeader.externalModuleName)
        cachedFileInfo.crossFileReferencesHash.toProtoStream(this)
        ifNotNull(cachedFileInfo.dtsHash, ::writeInt64NoTag)
        commitJsIrModuleHeaderNames(cachedFileInfo.jsIrHeader)
    }

    private fun CachedFileInfo.commitFileInfo() = runIf(!isExportFileCachedInfo) {
        moduleHeaderArtifact?.useCodedOutput {
            ifNotNull(exportFileCachedInfo) { commitSingleFileInfo(it) }
            commitSingleFileInfo(this@commitFileInfo)
        }
    }

    private fun ModuleArtifact.loadFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo> {
        val moduleArtifact = this
        val headers = fileArtifact.loadJsIrModuleHeaders(moduleArtifact)

        val mainCachedFileInfo = CachedFileInfo(headers.mainHeader, this, fileArtifact)

        if (headers.exportHeader != null) {
            val tsDeclarationsHash = fileArtifact.loadJsIrFragments().exportFragment?.dts?.raw?.cityHash64()
            val cachedExportFileInfo = mainCachedFileInfo.readModuleHeaderCache { fetchFileInfoForExportedPart(mainCachedFileInfo) }
            mainCachedFileInfo.exportFileCachedInfo = if (cachedExportFileInfo?.dtsHash != tsDeclarationsHash) {
                CachedFileInfo(
                    headers.exportHeader,
                    moduleArtifact,
                    fileArtifact,
                    tsDeclarationsHash = tsDeclarationsHash,
                    isExportFileCachedInfo = true
                )
            } else {
                cachedExportFileInfo
            }
        }

        return listOfNotNull(mainCachedFileInfo.exportFileCachedInfo, mainCachedFileInfo)
    }

    private val CachedFileInfo.cachedFiles: CachedFileArtifacts?
        get() = jsFileArtifact?.let { CachedFileArtifacts(it, sourceMapFileArtifact, dtsFileArtifact) }

    override fun getMainModuleAndDependencies(cacheInfo: List<CachedFileInfo>) = null to cacheInfo

    override fun fetchCompiledJsCodeForNullCacheInfo() = PerFileEntryPointCompilationOutput()

    override fun fetchCompiledJsCode(cacheInfo: CachedFileInfo) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, sourceMapFile, tsDeclarationsFile) ->
            jsCodeFile.ifExists { this }
                ?.let { CompilationOutputsCached(it, sourceMapFile?.ifExists { this }, tsDeclarationsFile?.ifExists { this }) }
        }

    override fun commitCompiledJsCode(cacheInfo: CachedFileInfo, compilationOutputs: CompilationOutputsBuilt) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, jsMapFile, tsDeclarationsFile) ->
            tsDeclarationsFile?.writeIfNotNull(compilationOutputs.tsDefinitions?.raw)
            compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
        } ?: compilationOutputs

    override fun loadJsIrModule(cacheInfo: CachedFileInfo): JsIrModule {
        val fragments = cacheInfo.fileArtifact.loadJsIrFragments()
        return JsIrModule(
            cacheInfo.jsIrHeader.moduleName,
            cacheInfo.jsIrHeader.externalModuleName,
            listOf(if (cacheInfo.isExportFileCachedInfo) fragments.exportFragment!! else fragments.mainFragment)
        )
    }

    override fun loadProgramHeadersFromCache(): List<CachedFileInfo> {
        return moduleArtifacts
            .flatMap { module ->
                module.fileArtifacts.flatMap {
                    if (it.isModified())
                        module.loadFileInfoFor(it)
                    else
                        module.fetchFileInfoFor(it) ?: module.loadFileInfoFor(it)
                }
            }
            .onEach { headerToCachedInfo[it.jsIrHeader] = it }
    }

    override fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>) {
        for ((header, references) in crossModuleReferences) {
            val cachedInfo = headerToCachedInfo[header] ?: notFoundIcError("artifact for module ${header.moduleName}")

            val actualCrossModuleHash = references.crossModuleReferencesHashForIC()

            if (header.associatedModule == null && cachedInfo.crossFileReferencesHash != actualCrossModuleHash) {
                header.associatedModule = loadJsIrModule(cachedInfo)
            }

            header.associatedModule?.let {
                cachedInfo.crossFileReferencesHash = actualCrossModuleHash
                cachedInfo.commitFileInfo()
            }
        }
    }

    private data class CachedFileArtifacts(val jsCodeFile: File, val sourceMapFile: File?, val tsDeclarationsFile: File?)
    private data class LoadedJsIrModuleHeaders(val mainHeader: JsIrModuleHeader, val exportHeader: JsIrModuleHeader?)
}
