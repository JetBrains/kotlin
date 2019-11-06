package org.jetbrains.konan.resolve.konan

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbols
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbolsImpl
import com.jetbrains.swift.codeinsight.resolve.SwiftModule
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps
import org.jetbrains.konan.resolve.symbols.KtFileReferenceSymbol
import org.jetbrains.konan.resolve.translation.KtFrameworkTranslator
import org.jetbrains.plugins.gradle.util.GradleConstants

abstract class KonanSwiftModule : SwiftModule, UserDataHolder by UserDataHolderBase() {
    protected abstract val project: Project
    protected abstract fun konanBridgeFile(): KonanBridgeVirtualFile?

    override fun isSourceModule(): Boolean = true
    override fun getBridgeFile(name: String): VirtualFile? = null
    override fun getBridgedHeaders(): List<VirtualFile> = emptyList()
    override fun getDependencies(): List<SwiftModule> = emptyList()

    override fun getSymbol(): SwiftModuleSymbol? {
        val bridgeFile = konanBridgeFile() ?: return null

        val name = name
        val props = SymbolProps(project, bridgeFile, name, 0, SwiftAttributesInfo.EMPTY, null, null)
        return SwiftSourceModuleSymbol(props, name)
    }

    override fun buildModuleCache(): SwiftGlobalSymbols {
        val file = konanBridgeFile() ?: return SwiftGlobalSymbols.EMPTY

        val swiftSymbols = SwiftGlobalSymbolsImpl(this)
        val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(swiftSymbols)

        val psiManager = PsiManager.getInstance(project)
        KtFrameworkTranslator(project).translateModule(file).forEach { symbol ->
            when (symbol) {
                is SwiftSymbol -> processor.process(symbol)
                is KtFileReferenceSymbol -> processFile(symbol, psiManager, file, processor)
            }
        }

        return swiftSymbols
    }

    private fun processFile(
        symbol: KtFileReferenceSymbol,
        psiManager: PsiManager,
        bridgeFile: KonanBridgeVirtualFile,
        processor: SwiftGlobalSymbolsImpl.SymbolProcessor
    ) {
        val file = symbol.targetFile
        val psiFile = psiManager.findFile(file) ?: return
        val context = OCInclusionContext.empty(SwiftLanguageKind.SWIFT, psiFile)
        context.addProcessedFile(bridgeFile)
        FileSymbolTable.forFile(file, context)?.processFile(processor)
    }

    companion object {
        fun getAllKonanTargets(project: Project): List<String> {
            val projectNode = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID).first().externalProjectStructure as DataNode<*>
            return ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.MODULE).map { it.data.id }
        }
    }
}
