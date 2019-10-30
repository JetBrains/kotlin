package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.cpp.OCIncludeSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.model.PBXTarget
import com.jetbrains.cidr.xcode.model.XcodeMetaData
import com.jetbrains.swift.bridging.SwiftBridgingUtil
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbols
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbolsImpl
import com.jetbrains.swift.codeinsight.resolve.SwiftModule
import com.jetbrains.swift.lang.parser.SwiftFileType
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps
import org.jetbrains.konan.resolve.translation.KtFrameworkTranslator

class KonanSwiftSourceModule(
    private val configuration: OCResolveConfiguration,
    private val target: PBXTarget,
    private val parentConfiguration: OCResolveConfiguration
) : SwiftModule, UserDataHolder by UserDataHolderBase() {

    private val project: Project = configuration.project

    override fun getBridgeFile(name: String): VirtualFile? = null

    override fun getBridgedHeaders(): List<VirtualFile> = emptyList()

    override fun isSourceModule(): Boolean = true

    override fun getName(): String = SwiftBridgingUtil.getProductModuleName(configuration, target)

    override fun getSymbol(): SwiftModuleSymbol? {
        val name = name
        val file = KonanBridgeVirtualFile(AppCodeKonanBridgeTarget(target), name, project, 0)
        val props = SymbolProps(project, file, name, 0, SwiftAttributesInfo.EMPTY, null, null)
        return SwiftSourceModuleSymbol(props, name)
    }

    override fun getConfiguration(): OCResolveConfiguration = parentConfiguration

    override fun buildModuleCache(): SwiftGlobalSymbols {
        val buildConfig = XcodeMetaData.getBuildConfigurationFor(configuration) ?: return SwiftGlobalSymbols.EMPTY

        val target = buildConfig.target ?: return SwiftGlobalSymbols.EMPTY

        val swiftSymbols = SwiftGlobalSymbolsImpl(this)
        val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(swiftSymbols)

        val file = KonanBridgeVirtualFile(AppCodeKonanBridgeTarget(target), name, project, 0)
        val psiManager = PsiManager.getInstance(project)
        KtFrameworkTranslator(project).translateModule(file).forEach { symbol ->
            when (symbol) {
                is SwiftSymbol -> processor.process(symbol)
                is OCIncludeSymbol -> processInclude(symbol, psiManager, processor)
            }
        }

        return swiftSymbols
    }

    private fun processInclude(symbol: OCIncludeSymbol, psiManager: PsiManager, processor: SwiftGlobalSymbolsImpl.SymbolProcessor) {
        //todo [medbedev] don't remeber why do we need this
        val file = symbol.targetFile ?: return
        val psiFile = psiManager.findFile(file) ?: return
        val context = OCInclusionContext.empty(SwiftLanguageKind.SWIFT, psiFile)
        FileSymbolTable.forFile(file, context)?.processFile(processor)
    }

    override fun getDependencies(): List<SwiftModule> = emptyList()

    override fun getFiles(): Collection<VirtualFile> = getSourceAndDerivedFiles(target) ?: target.sourceFiles

    private fun getSourceAndDerivedFiles(target: PBXTarget): List<VirtualFile>? =
        XcodeMetaData.getBuildSettings(configuration)?.getSourceAndDerivedFiles(target, SwiftFileType.INSTANCE)
}
