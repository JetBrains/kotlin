package com.jetbrains.swift.codeinsight.resolve.module

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.CLanguageKind
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.lang.toolchains.CidrCompilerSwitches
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.xcode.frameworks.ApplePlatform
import com.jetbrains.cidr.xcode.frameworks.AppleSdk
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.swift.codeinsight.resolve.MobileSwiftModuleManager
import com.jetbrains.swift.codeinsight.resolve.SwiftLibraryModule
import com.jetbrains.swift.languageKind.SwiftLanguageKind
import java.util.*
import kotlin.collections.HashSet

class MobileSwiftModuleReader(ioCache: SwiftModuleIOCache) : SwiftModuleReaderImpl<OCResolveConfiguration>(ioCache) {
    override fun doReadModule(
        configuration: OCResolveConfiguration,
        moduleName: String,
        customArgs: List<String>
    ): SwiftLibraryModule {

        if (ApplicationManager.getApplication().isUnitTestMode) {
            if (!SwiftCacheBuilder.shouldBuildModule(moduleName)) return emptyModule(configuration, moduleName)
        }

        val project = configuration.project

        val sdkInfo: SdkInfo? = MobileSwiftModuleManager.getAppleSdkInfo(configuration)
        val deploymentTarget = AppleSdkManager.getInstance().findSdksForPlatform(ApplePlatform.Type.IOS_SIMULATOR).firstOrNull()
            ?.versionString // XcodeMetaData.getBuildSettings(configuration)?.deploymentTarget
        val llvmTargetTripleOsVersion = null //XcodeMetaData.getBuildSettings(configuration)?.llvmTargetTripleOsVersion
        val llvmTargetTripleSuffix = null // XcodeMetaData.getBuildSettings(configuration)?.llvmTargetTripleSuffix
        val moduleInfo = ModuleInfo(moduleName, sdkInfo, deploymentTarget, llvmTargetTripleOsVersion, llvmTargetTripleSuffix)

        when (sdkInfo) {
            null -> {
                val files: List<SwiftLibraryModuleFile> = tryCustomModule(moduleInfo, configuration, project, customArgs)
                return SwiftLibraryModule(moduleInfo, configuration, files)
            }
            else -> {
                if (!isSwiftSupported(sdkInfo, deploymentTarget)) return emptyModule(configuration, moduleName)

                val files: List<VirtualFile> = tryMemoryCacheOrFallbackToInstalledCache(configuration, moduleInfo, customArgs)
                return SwiftLibraryModule(moduleInfo, configuration, files)
            }
        }
    }

    private fun tryCustomModule(
        moduleInfo: ModuleInfo,
        configuration: OCResolveConfiguration,
        project: Project,
        customArgs: List<String>
    ): List<SwiftLibraryModuleFile> {

        val inMemoryModuleCache = SwiftInMemoryModuleCache.getInstance(project)

        inMemoryModuleCache[moduleInfo]?.let { cachedCustomModule ->
            return cachedCustomModule
        }

        val files = readCustomModule(moduleInfo, configuration, customArgs)

        return inMemoryModuleCache.cacheOrGet(moduleInfo, files)
    }

    private fun tryMemoryCacheOrFallbackToInstalledCache(
        configuration: OCResolveConfiguration,
        moduleInfo: ModuleInfo,
        customArgs: List<String>
    ): List<VirtualFile> {
        if (moduleInfo.sdkInfo == null) {
            OCLog.LOG.error("sdk info must not be null")
            return Collections.emptyList()
        }

        val project = configuration.project
        val inMemoryModuleCache = SwiftInMemoryModuleCache.getInstance(project)

        inMemoryModuleCache.get(moduleInfo)?.let { cached ->
            return cached
        }

        return tryInstalledCacheOrFallbackToIOCache(configuration, moduleInfo, customArgs)
    }

    private fun tryInstalledCacheOrFallbackToIOCache(
        configuration: OCResolveConfiguration,
        moduleInfo: ModuleInfo,
        customArgs: List<String>
    ): List<VirtualFile> {
        myIOCache.readModuleInInstalledModuleCache(moduleInfo)?.let { prebuiltFiles ->
            if (prebuiltFiles.isNotEmpty()) {
                return prebuiltFiles
            }
        }

        return tryIOCacheOrFallbackToSourceKit(configuration, moduleInfo, customArgs)
    }

    private fun tryIOCacheOrFallbackToSourceKit(
        configuration: OCResolveConfiguration,
        moduleInfo: ModuleInfo,
        customArgs: List<String>
    ): List<VirtualFile> {
        myIOCache.readModule(moduleInfo)?.let { cachedFiles ->
            if (cachedFiles.isNotEmpty()) {
                return cachedFiles
            }
        }

        val project = configuration.project
        val args = prepareArgsForSDK(moduleInfo, project)

        val content: SwiftModuleContent? = invokeReadingAndWait(moduleInfo, customArgs + args)

        if (content == null || SwiftEmptyModules.isEmptyContent(moduleInfo, content)) {
            val files = readCustomModule(moduleInfo, configuration, args)
            SwiftInMemoryModuleCache.getInstance(project).cacheOrGet(moduleInfo, files)
            return files
        }

        myIOCache.cacheModule(moduleInfo, content, project)

        val files = SwiftLibraryModuleFile.createVirtualFiles(moduleInfo, content)
        return SwiftInMemoryModuleCache.getInstance(project).cacheOrGet(moduleInfo, files)
    }

    private fun isSwiftSupported(sdkInfo: SdkInfo, deploymentTarget: String?): Boolean {
        if (deploymentTarget != null) {
            val version = Version.parseVersion(deploymentTarget)
            if (version != null && ("ios" == sdkInfo.platform && version.lessThan(7) ||
                        "macosx" == sdkInfo.platform && version.lessThan(10, 9))
            ) {
                return false
            }
        }

        return true
    }

    private fun readCustomModule(
        moduleInfo: ModuleInfo,
        configuration: OCResolveConfiguration,
        initialArgs: List<String>
    ): List<SwiftLibraryModuleFile> {
        val args = ArrayList(initialArgs)

        val configurations: Set<String> = collectFrameworks(configuration)
        for (c in configurations) {
            args.add("-F")
            args.add(c)
        }

        val switches = configuration.getCompilerSettings(SwiftLanguageKind.SWIFT).compilerSwitches?.getList(CidrCompilerSwitches.Format.RAW)
        if (switches != null) {
            args.addAll(switches)
        }

        val content: SwiftModuleContent? = invokeReadingAndWait(moduleInfo, args)
        return convertToVirtualFiles(moduleInfo, content)
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