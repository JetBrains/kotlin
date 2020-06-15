package com.jetbrains.swift.codeinsight.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.modulemap.mainFile
import com.jetbrains.cidr.lang.modulemap.resolve.ModuleMapWalker
import com.jetbrains.cidr.lang.modulemap.symbols.ModuleMapSymbol
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.headerRoots.AppleFramework
import com.jetbrains.swift.symbols.SwiftModuleSymbol

class SwiftObjcFrameworkModule(
    private val framework: AppleFramework,
    private val configuration: OCResolveConfiguration
) : UserDataHolderBase(), SwiftModule {
    private val project: Project = configuration.project
    private val file: VirtualFile? = framework.mainFile
    private val symbol: SwiftModuleSymbol? = file?.let {
        SwiftLibraryModule.createLibraryModuleSymbol(framework.name, project, configuration, file)
    }

    @get:JvmName("files")
    private val files: List<VirtualFile> by lazy {
        val result = mutableListOf<VirtualFile>()

        ModuleMapWalker.buildModuleProcessor(project)
            .setConfiguration(configuration)
            .setModule(framework.name)
            .notVisitExportedModules()
            .process { module: ModuleMapSymbol ->
                result.addAll(module.includeHeaders)
                true
            }

        return@lazy result
    }

    override fun getSymbol(): SwiftModuleSymbol? = symbol

    override fun getName(): String = framework.name

    override fun isSourceModule(): Boolean = false

    override fun getFiles(): List<VirtualFile> = files

    override fun getDependencies(): Set<SwiftModule> = emptySet()

    override fun getConfiguration(): OCResolveConfiguration = configuration

    override fun buildModuleCache(): SwiftGlobalSymbols = MobileSwiftBridgingUtil.buildBridgedSymbols(this)

    override fun getSwiftInterfaceHeader(name: String): VirtualFile? = null

    override fun getBridgingHeaders(): List<VirtualFile> = files

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val module = other as SwiftObjcFrameworkModule

        return configuration == module.configuration &&
                framework == module.framework
    }

    override fun hashCode(): Int {
        var result = framework.hashCode()
        result = 31 * result + configuration.hashCode()
        return result
    }
}

