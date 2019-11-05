package org.jetbrains.konan.bridging

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.cidr.apple.bridging.MobileKonanTarget
import com.jetbrains.cidr.lang.preprocessor.OCInclusionContext
import com.jetbrains.cidr.lang.symbols.cpp.OCIncludeSymbol
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbols
import com.jetbrains.swift.codeinsight.resolve.SwiftGlobalSymbolsImpl
import com.jetbrains.swift.codeinsight.resolve.SwiftModule
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.SwiftSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps
import org.jetbrains.konan.resolve.konan.KonanBridgeFileManager
import org.jetbrains.konan.resolve.konan.KonanBridgeVirtualFile
import org.jetbrains.konan.resolve.translation.KtFrameworkTranslator
import org.jetbrains.plugins.gradle.util.GradleConstants

class MobileKonanSwiftModule(
    private val target: MobileKonanTarget,
    private val parentConfiguration: OCResolveConfiguration
) : SwiftModule, UserDataHolder by UserDataHolderBase() {
    private val project: Project
        get() = parentConfiguration.project

    override fun getBridgeFile(name: String): VirtualFile? = null

    override fun getBridgedHeaders(): List<VirtualFile> = emptyList()

    override fun isSourceModule(): Boolean = true

    override fun getName(): String = target.productModuleName

    override fun getSymbol(): SwiftModuleSymbol? {
        val name = name
        val props = SymbolProps(project, konanBridgeFile(), name, 0, SwiftAttributesInfo.EMPTY, null, null)
        return SwiftSourceModuleSymbol(props, name)
    }

    override fun getConfiguration(): OCResolveConfiguration = parentConfiguration

    override fun buildModuleCache(): SwiftGlobalSymbols {
        val swiftSymbols = SwiftGlobalSymbolsImpl(this)
        val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(swiftSymbols)

        val file = konanBridgeFile()
        val psiManager = PsiManager.getInstance(project)
        KtFrameworkTranslator(project).translateModule(file).forEach { symbol ->
            when (symbol) {
                is SwiftSymbol -> processor.process(symbol)
                is OCIncludeSymbol -> processInclude(symbol, psiManager, file, processor)
            }
        }

        return swiftSymbols
    }

    private fun konanBridgeFile(): KonanBridgeVirtualFile =
        KonanBridgeFileManager.getInstance(project).forTarget(target, name.replace('-', '_').let { "$it/$it.h" })

    private fun processInclude(
        symbol: OCIncludeSymbol,
        psiManager: PsiManager,
        bridgeFile: KonanBridgeVirtualFile,
        processor: SwiftGlobalSymbolsImpl.SymbolProcessor
    ) {
        //todo [medvedev] don't remember why do we need this
        val file = symbol.targetFile ?: return
        val psiFile = psiManager.findFile(file) ?: return
        val context = OCInclusionContext.empty(SwiftLanguageKind.SWIFT, psiFile)
        context.addProcessedFile(bridgeFile)
        FileSymbolTable.forFile(file, context)?.processFile(processor)
    }

    override fun getDependencies(): List<SwiftModule> = emptyList()

    override fun getFiles(): List<VirtualFile> = emptyList()

    companion object {
        internal fun getAllKonanTargets(project: Project): List<String> {
            val projectNode = ProjectDataManager.getInstance().getExternalProjectsData(project, GradleConstants.SYSTEM_ID).first()
                .externalProjectStructure as DataNode<*>
            return ExternalSystemApiUtil.findAll(projectNode, ProjectKeys.MODULE).map { it.data.id }
        }
    }
}
