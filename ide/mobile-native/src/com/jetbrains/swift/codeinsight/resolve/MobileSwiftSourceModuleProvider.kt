package com.jetbrains.swift.codeinsight.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.swift.codeinsight.resolve.module.SwiftSourceModuleFile
import com.jetbrains.swift.psi.SwiftFile
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps
import java.util.*

class MobileSwiftSourceModuleProvider : SwiftSourceModuleProvider {
    override fun isAvailable(configuration: OCResolveConfiguration): Boolean =
        GradleAppleWorkspace.getInstance(configuration.project).isOwnerOf(configuration)
                && configuration.sourceUrls.any { it.endsWith(".swift", true) }

    override fun createModule(configuration: OCResolveConfiguration): SwiftModule = object : SwiftModule, UserDataHolderBase() {
        private val project: Project
            get() = configuration.project

        private val cachedBridgedSymbols: CachedValue<SwiftGlobalSymbols> = CachedValuesManager.getManager(project).createCachedValue(
            {
                val tracker = FileSymbolTablesCache.getInstance(project).ocOutOfBlockModificationTracker
                val lazySymbols = SwiftLazyBridgedSymbols(this) { MobileSwiftBridgingUtil.buildBridgedSymbols(this) }
                Result.create<SwiftGlobalSymbols>(lazySymbols, tracker)
            }, false
        )

        override fun getBridgeFile(path: String): VirtualFile? = null // TODO

        override fun isSourceModule(): Boolean = true

        override fun getName(): String = configuration.name

        override fun getSymbol(): SwiftModuleSymbol = name.let { name ->
            val props = SymbolProps(project, SwiftSourceModuleFile(name), name, 0, SwiftAttributesInfo.EMPTY, null, null)
            return SwiftSourceModuleSymbol(props, name)
        }

        override fun getConfiguration(): OCResolveConfiguration = configuration

        override fun buildModuleCache(): SwiftGlobalSymbols {
            val swiftSymbols = SwiftGlobalSymbolsImpl(this)
            val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(swiftSymbols)

            for (file in files) {
                val psiFile = PsiManager.getInstance(project).findFile(file)
                if (psiFile !is SwiftFile) continue

                val context = SwiftResolveUtil.getInclusionContext(psiFile, configuration)
                FileSymbolTable.forFile(psiFile, context)?.processFile(processor) ?: OCLog.LOG.error("No table for file: " + file.path)
            }
            swiftSymbols.compact()

            val bridgedSymbols = cachedBridgedSymbols.value
            if (bridgedSymbols === SwiftGlobalSymbols.EMPTY) return swiftSymbols

            return SwiftAndBridgedSymbols(swiftSymbols, bridgedSymbols, this)
        }

        override fun getDependencies(): List<SwiftModule> = Collections.emptyList()

        override fun getFiles(): List<VirtualFile> = configuration.sources.let { sources: Collection<VirtualFile> ->
            sources as? List<VirtualFile> ?: sources.toList()
        }

        override fun getBridgedHeaders(): List<VirtualFile> =
            listOfNotNull(GradleAppleWorkspace.getInstance(project).getBridgingHeader(configuration))
    }
}
