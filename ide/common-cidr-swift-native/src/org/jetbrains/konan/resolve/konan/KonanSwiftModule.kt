package org.jetbrains.konan.resolve.konan

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
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.psi.types.SwiftPlace
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps
import org.jetbrains.konan.resolve.konan.KonanTarget.Companion.PRODUCT_MODULE_NAME_KEY

abstract class KonanSwiftModule : SwiftModule, UserDataHolder by UserDataHolderBase() {
    protected abstract val project: Project
    abstract fun konanBridgeFile(): KonanBridgeVirtualFile?

    override fun isSourceModule(): Boolean = true
    override fun getSwiftInterfaceHeader(name: String): VirtualFile? = null
    override fun getBridgingHeaders(): List<VirtualFile> = emptyList()
    override fun getDependencies(): Set<SwiftModule> = emptySet()

    override fun getSymbol(): SwiftModuleSymbol? {
        val bridgeFile = konanBridgeFile() ?: return null

        val name = name
        val props = SymbolProps(SwiftContext.interned(bridgeFile, project), name, 0, SwiftAttributesInfo.EMPTY, null)
        return SwiftSourceModuleSymbol(props, name)
    }

    override fun buildModuleCache(): SwiftGlobalSymbols {
        val file = konanBridgeFile() ?: return SwiftGlobalSymbols.EMPTY
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return SwiftGlobalSymbols.EMPTY
        val context = OCInclusionContext.empty(SwiftLanguageKind, psiFile)
        context.define(PRODUCT_MODULE_NAME_KEY, file.target.productModuleName)
        val table = FileSymbolTable.forFile(file, context)?.takeIf { !it.isEmpty } ?: return SwiftGlobalSymbols.EMPTY

        val bridgedSymbols = SwiftGlobalSymbolsImpl(SwiftGlobalSymbols.SymbolsOrigin.OBJC, this)
        val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(bridgedSymbols, SwiftPlace.Companion.of(project), false)
        val state = FileSymbolTable.ProcessingState(context, false)
        table.processSymbols(processor, null, state, null, null, null)

        if (!processor.isAnyProcessed) return SwiftGlobalSymbols.EMPTY
        bridgedSymbols.compact()
        return bridgedSymbols
    }
}
