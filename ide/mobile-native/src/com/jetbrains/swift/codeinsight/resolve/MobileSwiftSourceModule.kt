package com.jetbrains.swift.codeinsight.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.cidr.apple.bridging.MobileBridgeTarget
import com.jetbrains.cidr.apple.gradle.AppleTargetModel
import com.jetbrains.cidr.apple.gradle.GradleAppleWorkspace
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTable
import com.jetbrains.cidr.lang.symbols.symtable.FileSymbolTablesCache
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import com.jetbrains.mobile.bridging.MobileKonanSwiftModule
import com.jetbrains.swift.codeinsight.resolve.module.SwiftSourceModuleFile
import com.jetbrains.swift.psi.SwiftFile
import com.jetbrains.swift.psi.types.SwiftContext
import com.jetbrains.swift.symbols.SwiftAttributesInfo
import com.jetbrains.swift.symbols.SwiftBridgeVirtualFile
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import com.jetbrains.swift.symbols.impl.SwiftSourceModuleSymbol
import com.jetbrains.swift.symbols.impl.SymbolProps

class MobileSwiftSourceModule(private val config: OCResolveConfiguration) : SwiftModule, UserDataHolderBase() {
    private val project: Project
        get() = config.project

    private val target: AppleTargetModel = GradleAppleWorkspace.getInstance(project).getTarget(config)!!

    /** Contains the source module ID for test modules, and nothing for source modules */
    private val dependencyModuleIDs: List<String> = listOfNotNull(GradleAppleWorkspace.sourceConfigurationID(configuration.uniqueId))

    private val cachedBridgedSymbols: CachedValue<SwiftGlobalSymbols> = CachedValuesManager.getManager(project).createCachedValue(
        {
            val tracker = FileSymbolTablesCache.getInstance(project).outOfBlockModificationTracker
            val lazySymbols = SwiftLazyBridgedSymbols(this) {
                MobileSwiftBridgingUtil.buildBridgedSymbols(this)
            }
            CachedValueProvider.Result.create<SwiftGlobalSymbols>(
                lazySymbols,
                tracker
            )
        }, false
    )

    override fun getSwiftInterfaceHeader(path: String): VirtualFile? = when {
        FileUtil.pathsEqual(path, "${target.name}-Swift.h") ->
            SwiftBridgeVirtualFile.forTarget(MobileBridgeTarget(target), path, project)
        else -> null
    }

    override fun isSourceModule(): Boolean = true

    override fun getName(): String = config.name

    override fun getSymbol(): SwiftModuleSymbol = name.let { name ->
        val props = SymbolProps(SwiftContext.interned(SwiftSourceModuleFile(name), project), name, 0, SwiftAttributesInfo.EMPTY, null)
        return SwiftSourceModuleSymbol(props, name)
    }

    override fun getConfiguration(): OCResolveConfiguration = config

    override fun buildModuleCache(): SwiftGlobalSymbols {
        val swiftSymbols = SwiftGlobalSymbolsImpl(this)
        val processor = SwiftGlobalSymbolsImpl.SymbolProcessor(swiftSymbols)

        for (file in files) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile !is SwiftFile) continue

            val context = SwiftResolveUtil.getInclusionContext(psiFile, config)
            FileSymbolTable.forFile(psiFile, context)?.processFile(processor) ?: OCLog.LOG.error("No table for file: " + file.path)
        }
        swiftSymbols.compact()

        val bridgedSymbols = cachedBridgedSymbols.value
        if (bridgedSymbols === SwiftGlobalSymbols.EMPTY) return swiftSymbols

        return SwiftAndBridgedSymbols(swiftSymbols, bridgedSymbols, this)
    }

    override fun getDependencies(): List<SwiftModule> =
        GradleAppleWorkspace.getInstance(project).availableKonanFrameworkTargets.values.map { MobileKonanSwiftModule(it, config) } +
                dependencyModuleIDs.mapNotNull { id ->
                    OCWorkspace.getInstance(project).getConfigurationById(id)?.let { configuration ->
                        SwiftModuleManager.getInstance(project).getSourceModule(configuration)
                    }
                }

    override fun getFiles(): List<VirtualFile> = config.sources.let { sources: Collection<VirtualFile> ->
        sources as? List<VirtualFile> ?: sources.toList()
    }

    override fun getBridgingHeaders(): List<VirtualFile> = listOfNotNull(GradleAppleWorkspace.getInstance(project).getBridgingHeader(config))
}