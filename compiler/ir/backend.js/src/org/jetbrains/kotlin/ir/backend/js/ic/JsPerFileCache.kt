/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
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

        private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

        private fun JsIrProgramFragment.getMainFragmentExternalName(moduleArtifact: ModuleArtifact) =
            moduleFragmentToExternalName.getExternalNameFor(name, packageFqn, moduleArtifact.moduleExternalName)

        private fun JsIrProgramFragment.getExportFragmentExternalName(moduleArtifact: ModuleArtifact) =
            moduleFragmentToExternalName.getExternalNameForExporterFile(name, packageFqn, moduleArtifact.moduleExternalName)

        private fun SrcFileArtifact.loadJsIrModuleHeaders(moduleArtifact: ModuleArtifact) = with(loadJsIrFragments()!!) {
            LoadedJsIrModuleHeaders(
                mainFragment.mainFunction,
                mainFragment.run {
                    asIrModuleHeader(
                        getMainFragmentExternalName(moduleArtifact),
                        importWithEffectIn = runIf(hasEffect) { moduleArtifact.moduleExternalName }
                    )
                },
                exportFragment?.run {
                    asIrModuleHeader(
                        mainFragment.getExportFragmentExternalName(moduleArtifact),
                        reexportedIn = moduleArtifact.moduleExternalName
                    )
                },
            )
        }

        private fun JsIrProgramFragment.asIrModuleHeader(
            moduleName: String,
            reexportedIn: String? = null,
            importWithEffectIn: String? = null,
        ): JsIrModuleHeader {
            return JsIrModuleHeader(
                moduleName = moduleName,
                externalModuleName = moduleName,
                definitions = definitions,
                nameBindings = nameBindings.mapValues { v -> v.value.toString() },
                optionalCrossModuleImports = optionalCrossModuleImports,
                reexportedInModuleWithName = reexportedIn,
                importedWithEffectInModuleWithName = importWithEffectIn,
                associatedModule = JsIrModule(moduleName, moduleName, listOf(this), reexportedIn)
            )
        }
    }

    sealed class CachedFileInfo(val moduleArtifact: ModuleArtifact, moduleHeader: JsIrModuleHeader?) : CacheInfo {
        var crossFileReferencesHash: ICHash = ICHash()
        final override lateinit var jsIrHeader: JsIrModuleHeader

        init {
            if (moduleHeader != null) jsIrHeader = moduleHeader
        }

        abstract fun loadJsIrModule(): JsIrModule

        sealed class SerializableCachedFileInfo(
            moduleArtifact: ModuleArtifact,
            val fileArtifact: SrcFileArtifact,
            moduleHeader: JsIrModuleHeader?
        ) : CachedFileInfo(moduleArtifact, moduleHeader) {
            fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "$filePrefix.$name") }
            protected open val filePrefix by lazy(LazyThreadSafetyMode.NONE) { fileArtifact.srcFilePath.run { "${substringAfterLast('/')}.${cityHash64()}" } }

            override fun loadJsIrModule(): JsIrModule {
                val fragments = fileArtifact.loadJsIrFragments()!!
                val isExportFileCachedInfo = this is ExportFileCachedInfo
                return JsIrModule(
                    jsIrHeader.moduleName,
                    jsIrHeader.externalModuleName,
                    listOf(if (isExportFileCachedInfo) fragments.exportFragment!! else fragments.mainFragment),
                    runIf(isExportFileCachedInfo) { moduleArtifact.moduleSafeName }
                )
            }
        }

        open class MainFileCachedInfo(moduleArtifact: ModuleArtifact, fileArtifact: SrcFileArtifact, moduleHeader: JsIrModuleHeader? = null) :
            SerializableCachedFileInfo(moduleArtifact, fileArtifact, moduleHeader) {
            var mainFunctionTag: String? = null
            var exportFileCachedInfo: ExportFileCachedInfo? = null

            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS) }
            val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }
            val sourceMapFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS_MAP) }

            class Merged(private val cachedFileInfos: List<MainFileCachedInfo>) :
                MainFileCachedInfo(cachedFileInfos.first().moduleArtifact, cachedFileInfos.first().fileArtifact) {
                override val filePrefix by lazy(LazyThreadSafetyMode.NONE) {
                    val hash = cachedFileInfos.map { it.fileArtifact.srcFilePath }.sorted().joinToString().cityHash64()
                    fileArtifact.srcFilePath.run { "${substringAfterLast('/')}.$hash.merged" }
                }

                override fun loadJsIrModule(): JsIrModule = cachedFileInfos.map { it.loadJsIrModule() }.merge()

                init {
                    assert(cachedFileInfos.size > 1) { "Merge is unnecessary" }
                    val isModified = cachedFileInfos.any { it.fileArtifact.isModified() }
                    val mainAndExportHeaders = when {
                        isModified -> cachedFileInfos.asSequence().map { it.fileArtifact.loadJsIrModuleHeaders(moduleArtifact) }
                        else -> cachedFileInfos.asSequence().map {
                            LoadedJsIrModuleHeaders(
                                it.mainFunctionTag,
                                it.jsIrHeader,
                                it.exportFileCachedInfo?.jsIrHeader
                            )
                        }
                    }

                    val mainHeaders = mutableListOf<JsIrModuleHeader>()
                    val exportHeaders = mutableListOf<JsIrModuleHeader>()

                    for (loadedIrModuleHeaders in mainAndExportHeaders) {
                        mainHeaders.add(loadedIrModuleHeaders.mainHeader)

                        loadedIrModuleHeaders.exportHeader
                            ?.let { exportHeaders.add(it) }

                        loadedIrModuleHeaders.mainFunctionTag
                            .takeIf { mainFunctionTag == null }
                            ?.let { mainFunctionTag = it }
                    }

                    jsIrHeader = mainHeaders.merge()
                    exportFileCachedInfo = exportHeaders
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            ExportFileCachedInfo.Merged(
                                filePrefix,
                                it.merge(),
                                cachedFileInfos.mapNotNull(MainFileCachedInfo::exportFileCachedInfo)
                            )
                        }
                }
            }
        }

        open class ExportFileCachedInfo(
            moduleArtifact: ModuleArtifact,
            fileArtifact: SrcFileArtifact,
            moduleHeader: JsIrModuleHeader? = null,
            var tsDeclarationsHash: Long? = null
        ) : SerializableCachedFileInfo(moduleArtifact, fileArtifact, moduleHeader) {
            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_EXPORT_FILE_JS) }
            val dtsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_D_TS) }

            class Merged(
                override val filePrefix: String,
                moduleHeader: JsIrModuleHeader,
                private val cachedFileInfos: List<ExportFileCachedInfo>,
            ) : ExportFileCachedInfo(cachedFileInfos.first().moduleArtifact, cachedFileInfos.first().fileArtifact, moduleHeader) {
                override fun loadJsIrModule(): JsIrModule = cachedFileInfos.map { it.loadJsIrModule() }.merge()
            }
        }

        class ModuleProxyFileCachedInfo(moduleArtifact: ModuleArtifact, moduleHeader: JsIrModuleHeader? = null) :
            CachedFileInfo(moduleArtifact, moduleHeader) {
            var mainFunctionTag: String? = null

            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS) }
            val dtsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_D_TS) }
            val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }

            private fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "entry.$name") }

            override fun loadJsIrModule(): JsIrModule {
                return generateProxyIrModuleWith(
                    jsIrHeader.externalModuleName,
                    jsIrHeader.externalModuleName,
                    mainFunctionTag,
                    jsIrHeader.importedWithEffectInModuleWithName
                )
            }
        }
    }

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedFileInfo>()

    private fun <T : CachedFileInfo> CodedInputStream.loadSingleCachedFileInfo(cachedFileInfo: T): T = cachedFileInfo.also {
        val moduleName = readString()
        var reexportedIn: String? = null

        it.crossFileReferencesHash = ICHash.fromProtoStream(this)

        if (it is CachedFileInfo.ExportFileCachedInfo) {
            it.tsDeclarationsHash = runIf(readBool()) { readInt64() }
            reexportedIn = cachedFileInfo.moduleArtifact.moduleExternalName
        }


        val importWithEffectIn = ifTrue { readString() }
        val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

        it.jsIrHeader = JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings,
            optionalCrossModuleImports = optionalCrossModuleImports,
            reexportedInModuleWithName = reexportedIn,
            importedWithEffectInModuleWithName = importWithEffectIn,
            associatedModule = null,
        )
    }

    private fun <T> CachedFileInfo.MainFileCachedInfo.readModuleHeaderCache(f: CodedInputStream.() -> T): T? =
        moduleHeaderArtifact?.useCodedInputIfExists(f)

    private fun ModuleArtifact.fetchFileInfoFor(fileArtifact: SrcFileArtifact): CachedFileInfo.MainFileCachedInfo? {
        val mainFileCachedFileInfo = CachedFileInfo.MainFileCachedInfo(this, fileArtifact)

        return mainFileCachedFileInfo.readModuleHeaderCache {
            mainFileCachedFileInfo.apply {
                exportFileCachedInfo = fetchFileInfoForExportedPart(this)
                mainFunctionTag = ifTrue { readString() }
                loadSingleCachedFileInfo(this)
            }
        }
    }

    private fun ModuleArtifact.fetchModuleProxyFileInfo(): CachedFileInfo.ModuleProxyFileCachedInfo? {
        val mainFileCachedFileInfo = CachedFileInfo.ModuleProxyFileCachedInfo(this)
        return mainFileCachedFileInfo.moduleHeaderArtifact?.useCodedInputIfExists {
            mainFileCachedFileInfo.mainFunctionTag = ifTrue { readString() }
            loadSingleCachedFileInfo(mainFileCachedFileInfo)
        }
    }

    private fun CodedInputStream.fetchFileInfoForExportedPart(mainCachedFileInfo: CachedFileInfo.MainFileCachedInfo): CachedFileInfo.ExportFileCachedInfo? {
        return ifTrue {
            loadSingleCachedFileInfo(
                CachedFileInfo.ExportFileCachedInfo(mainCachedFileInfo.moduleArtifact, mainCachedFileInfo.fileArtifact)
            )
        }
    }

    private fun CodedOutputStream.commitSingleFileInfo(cachedFileInfo: CachedFileInfo) {
        writeStringNoTag(cachedFileInfo.jsIrHeader.externalModuleName)
        cachedFileInfo.crossFileReferencesHash.toProtoStream(this)
        if (cachedFileInfo is CachedFileInfo.ExportFileCachedInfo) {
            ifNotNull(cachedFileInfo.tsDeclarationsHash, ::writeInt64NoTag)
        }
        ifNotNull(cachedFileInfo.jsIrHeader.importedWithEffectInModuleWithName) { writeStringNoTag(it) }
        commitJsIrModuleHeaderNames(cachedFileInfo.jsIrHeader)
    }

    private fun CachedFileInfo.commitFileInfo() = when (this) {
        is CachedFileInfo.MainFileCachedInfo -> {
            moduleHeaderArtifact?.useCodedOutput {
                ifNotNull(exportFileCachedInfo) { commitSingleFileInfo(it) }
                ifNotNull(mainFunctionTag) { writeStringNoTag(it) }
                commitSingleFileInfo(this@commitFileInfo)
            }
        }
        is CachedFileInfo.ModuleProxyFileCachedInfo -> {
            moduleHeaderArtifact?.useCodedOutput {
                ifNotNull(mainFunctionTag) { writeStringNoTag(it) }
                commitSingleFileInfo(this@commitFileInfo)
            }
        }
        is CachedFileInfo.ExportFileCachedInfo -> {}
    }

    private fun ModuleArtifact.generateModuleProxyFileCachedInfo(
        mainFunctionTag: String?,
        importedWithEffectInModuleWithName: String? = null
    ): CachedFileInfo {
        val moduleHeader = generateProxyIrModuleWith(
            moduleExternalName,
            moduleExternalName,
            mainFunctionTag,
            importedWithEffectInModuleWithName
        ).makeModuleHeader()
        return CachedFileInfo.ModuleProxyFileCachedInfo(this, moduleHeader)
            .also { it.mainFunctionTag = mainFunctionTag }
    }

    private fun ModuleArtifact.loadFileInfoFor(fileArtifact: SrcFileArtifact): CachedFileInfo.MainFileCachedInfo {
        val headers = fileArtifact.loadJsIrModuleHeaders(this)

        val mainCachedFileInfo = CachedFileInfo.MainFileCachedInfo(this, fileArtifact, headers.mainHeader)
            .apply { mainFunctionTag = headers.mainFunctionTag }

        if (headers.exportHeader != null) {
            val tsDeclarationsHash = fileArtifact.loadJsIrFragments()?.exportFragment?.dts?.raw?.cityHash64()
            val cachedExportFileInfo = mainCachedFileInfo.readModuleHeaderCache { fetchFileInfoForExportedPart(mainCachedFileInfo) }
            mainCachedFileInfo.exportFileCachedInfo = if (cachedExportFileInfo?.tsDeclarationsHash != tsDeclarationsHash) {
                CachedFileInfo.ExportFileCachedInfo(
                    this,
                    fileArtifact,
                    headers.exportHeader,
                    tsDeclarationsHash,
                )
            } else {
                cachedExportFileInfo
            }
        }

        return mainCachedFileInfo
    }

    private val CachedFileInfo.cachedFiles: CachedFileArtifacts?
        get() = when (this) {
            is CachedFileInfo.MainFileCachedInfo -> jsFileArtifact?.let { CachedFileArtifacts(it, sourceMapFileArtifact, null) }
            is CachedFileInfo.ExportFileCachedInfo -> jsFileArtifact?.let { CachedFileArtifacts(it, null, dtsFileArtifact) }
            is CachedFileInfo.ModuleProxyFileCachedInfo -> jsFileArtifact?.let { CachedFileArtifacts(it, null, dtsFileArtifact) }
        }

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

    override fun loadJsIrModule(cacheInfo: CachedFileInfo): JsIrModule = cacheInfo.loadJsIrModule()

    override fun loadProgramHeadersFromCache(): List<CachedFileInfo> {
        val mainModuleArtifact = moduleArtifacts.last()

        val perFileGenerator = object : PerFileGenerator<ModuleArtifact, SrcFileArtifact, CachedFileInfo> {
            override val mainModuleName get() = mainModuleArtifact.moduleExternalName

            override val ModuleArtifact.isMain get() = this === mainModuleArtifact
            override val ModuleArtifact.fileList get() = fileArtifacts

            override val CachedFileInfo.artifactName get() = jsIrHeader.externalModuleName
            override val CachedFileInfo.hasEffect get() = jsIrHeader.importedWithEffectInModuleWithName != null
            override val CachedFileInfo.hasExport get() = this is CachedFileInfo.MainFileCachedInfo && exportFileCachedInfo != null
            override val CachedFileInfo.packageFqn get() = moduleFragmentToExternalName.excludeFileNameFromExternalName(jsIrHeader.moduleName)
            override val CachedFileInfo.mainFunction
                get() = when (this) {
                    is CachedFileInfo.MainFileCachedInfo -> mainFunctionTag
                    is CachedFileInfo.ModuleProxyFileCachedInfo -> mainFunctionTag
                    else -> error("Unexpected CachedFileInfo type ${this::class.simpleName}")
                }

            override fun SrcFileArtifact.generateArtifact(module: ModuleArtifact) = when {
                isModified() -> module.loadFileInfoFor(this)
                else -> module.fetchFileInfoFor(this) ?: module.loadFileInfoFor(this)
            }

            override fun List<CachedFileInfo>.merge() = when (size) {
                0 -> error("Can't merge empty list of MainFileCachedInfo")
                1 -> first()
                else -> CachedFileInfo.MainFileCachedInfo.Merged(map { it as CachedFileInfo.MainFileCachedInfo })
            }

            override fun ModuleArtifact.generateArtifact(mainFunctionTag: String?, moduleNameForEffects: String?) =
                fetchModuleProxyFileInfo()?.takeIf {
                    it.mainFunction == mainFunctionTag && it.jsIrHeader.importedWithEffectInModuleWithName == moduleNameForEffects
                } ?: generateModuleProxyFileCachedInfo(mainFunctionTag, moduleNameForEffects)
        }

        return perFileGenerator.generatePerFileArtifacts(moduleArtifacts)
            .flatMap { if (it is CachedFileInfo.MainFileCachedInfo) listOfNotNull(it.exportFileCachedInfo, it) else listOf(it) }
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
    private data class LoadedJsIrModuleHeaders(
        val mainFunctionTag: String?,
        val mainHeader: JsIrModuleHeader,
        val exportHeader: JsIrModuleHeader?
    )
}