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

/**
 * This class maintains incremental cache files used by [JsExecutableProducer] for per-file compilation mode.
 */
class JsPerFileCache(private val moduleArtifacts: List<JsModuleArtifact>) : JsMultiArtifactCache<JsPerFileCache.CachedFileInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_FILE_JS = "file.js"
        private const val CACHED_EXPORT_FILE_JS = "file.export.js"
        private const val CACHED_FILE_JS_MAP = "file.js.map"
        private const val CACHED_FILE_D_TS = "file.d.ts"

        private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

        private fun JsIrProgramFragment.getMainFragmentExternalName(moduleArtifact: JsModuleArtifact) =
            moduleFragmentToExternalName.getExternalNameFor(name, packageFqn, moduleArtifact.moduleExternalName)

        private fun JsIrProgramFragment.getExportFragmentExternalName(moduleArtifact: JsModuleArtifact) =
            moduleFragmentToExternalName.getExternalNameForExporterFile(name, packageFqn, moduleArtifact.moduleExternalName)

        private fun JsSrcFileArtifact.loadJsIrModuleHeaders(moduleArtifact: JsModuleArtifact) = with(loadIrFragments()!!) {
            LoadedJsIrModuleHeaders(
                mainFragment.mainFunctionTag,
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

    sealed class CachedFileInfo(val moduleArtifact: JsModuleArtifact, moduleHeader: JsIrModuleHeader?) : CacheInfo {
        var crossFileReferencesHash: ICHash = ICHash()
        final override lateinit var jsIrHeader: JsIrModuleHeader

        init {
            if (moduleHeader != null) jsIrHeader = moduleHeader
        }

        abstract fun loadJsIrModule(): JsIrModule

        sealed class SerializableCachedFileInfo(
            moduleArtifact: JsModuleArtifact,
            val fileArtifact: JsSrcFileArtifact,
            moduleHeader: JsIrModuleHeader?
        ) : CachedFileInfo(moduleArtifact, moduleHeader) {
            fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "$filePrefix.$name") }
            protected open val filePrefix by lazy(LazyThreadSafetyMode.NONE) { fileArtifact.srcFilePath.run { "${substringAfterLast('/')}.${cityHash64()}" } }

            override fun loadJsIrModule(): JsIrModule {
                val fragments = fileArtifact.loadIrFragments()!!.also {
                    it.mainFragment.testEnvironment = null
                }

                val isExportFileCachedInfo = this is ExportFileCachedInfo
                return JsIrModule(
                    jsIrHeader.moduleName,
                    jsIrHeader.externalModuleName,
                    listOf(if (isExportFileCachedInfo) fragments.exportFragment!! else fragments.mainFragment),
                    runIf(isExportFileCachedInfo) { moduleArtifact.moduleSafeName }
                )
            }
        }

        open class MainFileCachedInfo(moduleArtifact: JsModuleArtifact, fileArtifact: JsSrcFileArtifact, moduleHeader: JsIrModuleHeader? = null) :
            SerializableCachedFileInfo(moduleArtifact, fileArtifact, moduleHeader) {
            var mainFunctionTag: String? = null
            var testEnvironment: JsIrProgramTestEnvironment? = null
            var exportFileCachedInfo: ExportFileCachedInfo? = null

            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS) }
            val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }
            val sourceMapFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS_MAP) }

            class Merged(private val cachedFileInfos: List<MainFileCachedInfo>) :
                MainFileCachedInfo(cachedFileInfos.first().moduleArtifact, cachedFileInfos.first().fileArtifact) {
                override fun loadJsIrModule(): JsIrModule = cachedFileInfos.map { it.loadJsIrModule() }.merge()

                override val filePrefix by lazy(LazyThreadSafetyMode.NONE) {
                    val hash = cachedFileInfos.map { it.fileArtifact.srcFilePath }.sorted().joinToString().cityHash64()
                    fileArtifact.srcFilePath.run { "${substringAfterLast('/')}.$hash.merged" }
                }

                init {
                    assert(cachedFileInfos.size > 1) { "Merge is unnecessary" }
                    var isModified = false

                    for (info in cachedFileInfos) {
                        if (!info.fileArtifact.isModified()) {
                            isModified = true
                        }
                        info.testEnvironment?.let { testEnvironment = it }
                    }

                    val mainAndExportHeaders = when {
                        isModified -> cachedFileInfos.asSequence().map { it.fileArtifact.loadJsIrModuleHeaders(moduleArtifact) }
                        else -> cachedFileInfos.asSequence().map { LoadedJsIrModuleHeaders(it.mainFunctionTag, it.jsIrHeader, it.exportFileCachedInfo?.jsIrHeader) }
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
                    exportFileCachedInfo = exportHeaders.takeIf { it.isNotEmpty() }?.let {
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
            moduleArtifact: JsModuleArtifact,
            fileArtifact: JsSrcFileArtifact,
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

        class ModuleProxyFileCachedInfo(moduleArtifact: JsModuleArtifact, moduleHeader: JsIrModuleHeader? = null) :
            CachedFileInfo(moduleArtifact, moduleHeader) {
            var mainFunctionTag: String? = null
            var suiteFunctionTag: String? = null
            var packagesToItsTestFunctions: CachedTestFunctionsWithTheirPackage = emptyMap()

            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS) }
            val dtsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_D_TS) }
            val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }

            private fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "entry.$name") }

            override fun loadJsIrModule(): JsIrModule {
                return generateProxyIrModuleWith(
                    jsIrHeader.externalModuleName,
                    jsIrHeader.externalModuleName,
                    mainFunctionTag,
                    suiteFunctionTag,
                    packagesToItsTestFunctions,
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

        when (it) {
            is CachedFileInfo.MainFileCachedInfo -> {
                it.mainFunctionTag = ifTrue { readString() }
                it.testEnvironment = ifTrue { JsIrProgramTestEnvironment(readString(), readString()) }
            }
            is CachedFileInfo.ExportFileCachedInfo -> {
                it.tsDeclarationsHash = ifTrue { readInt64() }
                reexportedIn = cachedFileInfo.moduleArtifact.moduleExternalName
            }
            is CachedFileInfo.ModuleProxyFileCachedInfo -> {
                it.mainFunctionTag = ifTrue { readString() }
                it.suiteFunctionTag = ifTrue { readString() }
                it.packagesToItsTestFunctions = loadTestFunctions()
            }
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

    private fun CodedInputStream.loadTestFunctions() = buildMap {
        repeat(readInt32()) {
            put(readString(), buildList {
                repeat(readInt32()) { add(readString()) }
            })
        }
    }

    private fun <T> CachedFileInfo.MainFileCachedInfo.readModuleHeaderCache(f: CodedInputStream.() -> T): T? =
        moduleHeaderArtifact?.useCodedInputIfExists(f)

    private fun JsModuleArtifact.fetchFileInfoFor(fileArtifact: JsSrcFileArtifact): CachedFileInfo.MainFileCachedInfo? {
        val mainFileCachedFileInfo = CachedFileInfo.MainFileCachedInfo(this, fileArtifact)

        return mainFileCachedFileInfo.readModuleHeaderCache {
            mainFileCachedFileInfo.apply {
                exportFileCachedInfo = fetchFileInfoForExportedPart(this)
                loadSingleCachedFileInfo(this)
            }
        }
    }

    private fun JsModuleArtifact.fetchModuleProxyFileInfo(): CachedFileInfo.ModuleProxyFileCachedInfo? {
        val mainFileCachedFileInfo = CachedFileInfo.ModuleProxyFileCachedInfo(this)
        return mainFileCachedFileInfo.moduleHeaderArtifact?.useCodedInputIfExists { loadSingleCachedFileInfo(mainFileCachedFileInfo) }
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
        when (cachedFileInfo) {
            is CachedFileInfo.MainFileCachedInfo -> {
                ifNotNull(cachedFileInfo.mainFunctionTag, ::writeStringNoTag)
                ifNotNull(cachedFileInfo.testEnvironment) {
                    writeStringNoTag(it.testFunctionTag)
                    writeStringNoTag(it.suiteFunctionTag)
                }
            }
            is CachedFileInfo.ExportFileCachedInfo -> ifNotNull(cachedFileInfo.tsDeclarationsHash, ::writeInt64NoTag)
            is CachedFileInfo.ModuleProxyFileCachedInfo -> {
                ifNotNull(cachedFileInfo.mainFunctionTag, ::writeStringNoTag)
                ifNotNull(cachedFileInfo.suiteFunctionTag, ::writeStringNoTag)
                writeTestFunctions(cachedFileInfo.packagesToItsTestFunctions)
            }
        }
        ifNotNull(cachedFileInfo.jsIrHeader.importedWithEffectInModuleWithName) { writeStringNoTag(it) }
        commitJsIrModuleHeaderNames(cachedFileInfo.jsIrHeader)
    }

    private fun CodedOutputStream.writeTestFunctions(cachedTestFunctionsWithTheirPackage: CachedTestFunctionsWithTheirPackage) {
        writeInt32NoTag(cachedTestFunctionsWithTheirPackage.size)
        cachedTestFunctionsWithTheirPackage.forEach { (key, value) ->
            writeStringNoTag(key)
            writeInt32NoTag(value.size)
            value.forEach(::writeStringNoTag)
        }
    }

    private fun CachedFileInfo.commitFileInfo() = when (this) {
        is CachedFileInfo.MainFileCachedInfo -> {
            moduleHeaderArtifact?.useCodedOutput {
                ifNotNull(exportFileCachedInfo) { commitSingleFileInfo(it) }
                commitSingleFileInfo(this@commitFileInfo)
            }
        }
        is CachedFileInfo.ModuleProxyFileCachedInfo -> {
            moduleHeaderArtifact?.useCodedOutput {
                commitSingleFileInfo(this@commitFileInfo)
            }
        }
        is CachedFileInfo.ExportFileCachedInfo -> {}
    }

    private fun JsModuleArtifact.generateModuleProxyFileCachedInfo(
        mainFunctionTag: String?,
        suiteFunctionTag: String?,
        cachedTestFunctionsWithTheirPackage: CachedTestFunctionsWithTheirPackage,
        importedWithEffectInModuleWithName: String? = null
    ): CachedFileInfo {
        val moduleHeader = generateProxyIrModuleWith(
            moduleExternalName,
            moduleExternalName,
            mainFunctionTag,
            suiteFunctionTag,
            cachedTestFunctionsWithTheirPackage,
            importedWithEffectInModuleWithName
        ).makeModuleHeader()
        return CachedFileInfo.ModuleProxyFileCachedInfo(this, moduleHeader)
            .also {
                it.mainFunctionTag = mainFunctionTag
                it.suiteFunctionTag = suiteFunctionTag
                it.packagesToItsTestFunctions = cachedTestFunctionsWithTheirPackage
            }
    }

    private fun JsModuleArtifact.loadFileInfoFor(fileArtifact: JsSrcFileArtifact): CachedFileInfo.MainFileCachedInfo {
        val headers = fileArtifact.loadJsIrModuleHeaders(this)
        val mainFragment =
            headers.mainHeader.associatedModule?.fragments?.single() ?: error("Unexpected multiple fragments inside mainHeader")

        val mainCachedFileInfo = CachedFileInfo.MainFileCachedInfo(this, fileArtifact, headers.mainHeader).apply {
            mainFunctionTag = headers.mainFunctionTag
            testEnvironment = mainFragment.testEnvironment
            mainFragment.testEnvironment = null
        }

        if (headers.exportHeader != null) {
            val tsDeclarationsHash = headers.exportHeader.associatedModule?.fragments?.single()?.dts?.raw?.cityHash64()
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

        val perFileGenerator = object : PerFileGenerator<JsModuleArtifact, JsSrcFileArtifact, CachedFileInfo> {
            override val mainModuleName get() = mainModuleArtifact.moduleExternalName

            override val JsModuleArtifact.isMain get() = this === mainModuleArtifact
            override val JsModuleArtifact.fileList get() = fileArtifacts

            override val CachedFileInfo.artifactName get() = jsIrHeader.externalModuleName
            override val CachedFileInfo.hasEffect get() = jsIrHeader.importedWithEffectInModuleWithName != null
            override val CachedFileInfo.hasExport get() = this is CachedFileInfo.MainFileCachedInfo && exportFileCachedInfo != null
            override val CachedFileInfo.packageFqn get() = moduleFragmentToExternalName.getPackageFqn(jsIrHeader.moduleName)
            override val CachedFileInfo.mainFunction
                get() = when (this) {
                    is CachedFileInfo.MainFileCachedInfo -> mainFunctionTag
                    is CachedFileInfo.ModuleProxyFileCachedInfo -> mainFunctionTag
                    else -> error("Unexpected CachedFileInfo type ${this::class.simpleName}")
                }

            override fun CachedFileInfo.takeTestEnvironmentOwnership() =
                (this as CachedFileInfo.MainFileCachedInfo).testEnvironment

            override fun JsSrcFileArtifact.generateArtifact(module: JsModuleArtifact) = when {
                isModified() -> module.loadFileInfoFor(this)
                else -> module.fetchFileInfoFor(this) ?: module.loadFileInfoFor(this)
            }

            override fun List<CachedFileInfo>.merge() = when (size) {
                0 -> error("Can't merge empty list of MainFileCachedInfo")
                1 -> first()
                else -> CachedFileInfo.MainFileCachedInfo.Merged(map { it as CachedFileInfo.MainFileCachedInfo })
            }

            override fun JsModuleArtifact.generateArtifact(
                mainFunctionTag: String?,
                suiteFunctionTag: String?,
                testFunctions: CachedTestFunctionsWithTheirPackage,
                moduleNameForEffects: String?
            ) = fetchModuleProxyFileInfo()?.takeIf {
                it.mainFunctionTag == mainFunctionTag
                        && it.jsIrHeader.importedWithEffectInModuleWithName == moduleNameForEffects
                        && suiteFunctionTag == it.suiteFunctionTag &&
                        it.packagesToItsTestFunctions == testFunctions &&
                        it.jsIrHeader.importedWithEffectInModuleWithName == moduleNameForEffects
            } ?: generateModuleProxyFileCachedInfo(mainFunctionTag, suiteFunctionTag, testFunctions, moduleNameForEffects)
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
