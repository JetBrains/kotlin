package com.jetbrains.swift.codeinsight.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Processor
import com.jetbrains.cidr.lang.modulemap.resolve.ModuleMapResolveService
import com.jetbrains.cidr.lang.modulemap.symbols.ModuleMapModuleSymbol
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.headerRoots.AppleFramework
import com.jetbrains.swift.symbols.SwiftModuleSymbol
import java.util.*

class SwiftObjcFrameworkModule(
    private val framework: AppleFramework,
    private val configuration: OCResolveConfiguration
) : UserDataHolderBase(), SwiftModule {
    private val project: Project = configuration.project
    private val file: VirtualFile? = framework.mainFile
    private val symbol: SwiftModuleSymbol? = file?.let {
        SwiftLibraryModule.createLibraryModuleSymbol(framework.name, project, configuration, file)
    }
    private val files: List<VirtualFile> by lazy {
        val result = mutableListOf<VirtualFile>()

        val service = ModuleMapResolveService.getInstance(project)
        service.processModules(
            framework.name,
            configuration,
            visitExportedModules = false,
            processor = Processor { module: ModuleMapModuleSymbol ->
                result += service.getIncludeHeaders(module, configuration)
                return@Processor true
            })

        return@lazy result
    }

    override fun getLibraryFiles(): List<VirtualFile> = files

    override fun getSymbol(): SwiftModuleSymbol? = symbol

    override fun getName(): String = framework.name

    override fun isSourceModule(): Boolean = false

    override fun getAllFiles(): List<VirtualFile> = libraryFiles

    override fun getDependencies(): List<SwiftModule> = Collections.emptyList()

    override fun getConfiguration(): OCResolveConfiguration = configuration

    override fun buildModuleCache(): SwiftGlobalSymbols =
        MobileSwiftBridgingUtil.buildBridgedSymbols(allFiles, configuration, name, project)

    override fun getBridgeFile(name: String): VirtualFile? = null

    override fun getBridgedHeaders(): List<VirtualFile> = files

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

