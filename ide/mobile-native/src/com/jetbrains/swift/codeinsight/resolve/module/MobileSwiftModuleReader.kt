package com.jetbrains.swift.codeinsight.resolve.module

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.modulemap.AllowedModules
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdk
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.swift.codeinsight.resolve.MobileSwiftModuleManager
import com.jetbrains.swift.codeinsight.resolve.SwiftLibraryModule
import com.jetbrains.swift.lang.SwiftNames
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import java.util.*
import kotlin.collections.HashSet

class MobileSwiftModuleReader : SwiftModuleReaderImpl<OCResolveConfiguration>() {
    override fun createModuleInfo(configuration: OCResolveConfiguration, moduleName: String): ModuleInfo =
            ModuleInfo(moduleName,
                    MobileSwiftModuleManager.getAppleSdkInfo(configuration),
                    AppleSdkManager.getInstance().findSdksForPlatform(ApplePlatform.Type.IOS_SIMULATOR).firstOrNull()
                            ?.versionString,
                    null,
                    null)

    override fun doReadModule(
        configuration: OCResolveConfiguration,
        moduleName: String,
        customArgs: List<String>
    ): SwiftLibraryModule {
        if (ApplicationManager.getApplication().isUnitTestMode &&
                moduleName != SwiftNames.SWIFT_MODULE &&
                !AllowedModules.allowedModules.contains(moduleName)) {
            return emptyModule(configuration, moduleName)
        }

        val project = configuration.project
        val moduleInfo = createModuleInfo(configuration, moduleName)

        val files = when (moduleInfo.sdkInfo) {
            null -> tryCustomModule(moduleInfo, configuration, project, customArgs)
            else -> tryMemoryCache(configuration, moduleInfo)
                    ?: tryInstalledCache(moduleInfo)
                    ?: tryIOCacheOrReadFromSourceKit(configuration, moduleInfo, customArgs)
        }

        return SwiftLibraryModule(moduleInfo, configuration, files)
    }

    private fun tryCustomModule(moduleInfo: ModuleInfo,
                                configuration: OCResolveConfiguration,
                                project: Project,
                                customArgs: List<String>): List<SwiftLibraryModuleFile> {

        val inMemoryModuleCache = SwiftInMemoryModuleCache.getInstance(project)

        inMemoryModuleCache[moduleInfo]?.let { cachedCustomModule ->
            return cachedCustomModule
        }

        val content = readCustomModule(moduleInfo, configuration, customArgs)
        val files = convertToVirtualFiles(moduleInfo, content)
        return inMemoryModuleCache.cacheOrGet(moduleInfo, files)
    }

    private fun tryMemoryCache(configuration: OCResolveConfiguration, moduleInfo: ModuleInfo): List<VirtualFile>? {
        if (moduleInfo.sdkInfo == null) {
            OCLog.LOG.error("sdk info must not be null")
            return emptyList()
        }

        val project = configuration.project
        val inMemoryModuleCache = SwiftInMemoryModuleCache.getInstance(project)

        return inMemoryModuleCache.get(moduleInfo)
    }

    private fun tryInstalledCache(moduleInfo: ModuleInfo): List<VirtualFile>? {
        return myIOCache.readModuleInInstalledModuleCache(moduleInfo)
    }

    private fun tryIOCacheOrReadFromSourceKit(configuration: OCResolveConfiguration,
                                              moduleInfo: ModuleInfo,
                                              customArgs: List<String>): List<VirtualFile> {
        myIOCache.readModule(moduleInfo)
                ?.takeIf { cachedFiles -> cachedFiles.isNotEmpty() }
                ?.let { cachedFiles ->
                    return cachedFiles
                }

        val project = configuration.project
        val args = prepareArgsForSDK(moduleInfo, project)

        val content: SwiftModuleContent? = invokeReadingAndWait(moduleInfo, customArgs + args)

        if (content == null || SwiftEmptyModules.isEmptyContent(moduleInfo, content)) {
            val customModuleContent = readCustomModule(moduleInfo, configuration, args)
            val files = convertToVirtualFiles(moduleInfo, customModuleContent)
            SwiftInMemoryModuleCache.getInstance(project).cacheOrGet(moduleInfo, files)
            return files
        }

        myIOCache.cacheModule(moduleInfo, content, project)

        val files = SwiftLibraryModuleFile.createVirtualFiles(moduleInfo, content)
        return SwiftInMemoryModuleCache.getInstance(project).cacheOrGet(moduleInfo, files)
    }

    private fun readCustomModule(moduleInfo: ModuleInfo,
                                 configuration: OCResolveConfiguration,
                                 initialArgs: List<String>): SwiftModuleContent? {
        val args = ArrayList(initialArgs)

        val configurations: Set<String> = collectFrameworks(configuration)
        for (c in configurations) {
            args.add("-F")
            args.add(c)
        }

        val switches = configuration.getCompilerSettings(SwiftLanguageKind).compilerSwitches?.getList(CidrCompilerSwitches.Format.RAW)
        if (switches != null) {
            args.addAll(switches)
        }

        return invokeReadingAndWait(moduleInfo, args)
    }

    private fun prepareArgsForSDK(
        moduleInfo: ModuleInfo,
        project: Project
    ): List<String> {
        val args = ArrayList<String>()
        val sdkInfo = moduleInfo.sdkInfo!!

        args.add("-module-cache-path")
        args.add(PathManager.getSystemPath() + "/ModuleCache")

        val llvmTargetTriple = moduleInfo.llvmTargetTriple
        if (llvmTargetTriple != null) {
            args.add("-target")
            args.add(llvmTargetTriple)
        }

        val sdk: AppleSdk? = AppleSdkManager.getInstance().findSdk(sdkInfo.sdk)
        if (sdk != null) {
            args.add("-sdk")
            args.add(sdk.homePath)
        } else {
            OCLog.LOG.warn("SDK not found " + sdkInfo.sdk)
        }

        return args
    }

    private fun collectFrameworks(config: OCResolveConfiguration): LinkedHashSet<String> {
        val frameworkPaths = linkedSetOf<String>()
        val frameworksRoots = config.getCompilerSettings(CLanguageKind.OBJ_C).headersSearchRoots.frameworksRoots

        for (item in frameworksRoots) {
            item.processFrameworks({ element ->
                                       val file = element.virtualFile
                                       val parent = file?.parent
                                       if (parent != null) frameworkPaths.add(parent.path)
                                       true
                                   }, HashSet(), false)
        }
        return frameworkPaths
    }
}