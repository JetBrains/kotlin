package com.jetbrains.swift.codeinsight.resolve.module

import com.intellij.openapi.util.Version
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.cidr.lang.OCLog
import com.jetbrains.cidr.xcode.Xcode
import com.jetbrains.cidr.xcode.frameworks.AppleSdkManager
import com.jetbrains.swift.codeinsight.resolve.MobileSwiftModuleManager
import java.util.*

class MobileSwiftModuleIOCache : SwiftModuleIOCacheImpl(Xcode.getBuildVersionString()) {
    private val mySDKCache = ContainerUtil.newConcurrentMap<VirtualFile, SdkInfo>()

    override fun getSDKInfoByCachedFile(cachedFile: VirtualFile): SdkInfo {
        val sdkDir = cachedFile.parent?.parent
        OCLog.LOG.assertTrue(sdkDir != null, cachedFile)

        val cached = mySDKCache[sdkDir!!]
        if (cached != null) return cached

        val info = inferSdkInfo(sdkDir)
        return ConcurrencyUtil.cacheOrGet<VirtualFile, SdkInfo>(mySDKCache, sdkDir, info)
    }

    private fun inferSdkInfo(sdkDir: VirtualFile): SdkInfo {
        val architectureDir = sdkDir.parent
        val versionDir = architectureDir.parent

        val sdkName = sdkDir.name
        val sdk = AppleSdkManager.getInstance().findSdk(sdkName)
        OCLog.LOG.assertTrue(sdk != null, sdkName)

        return SdkInfo(
            sdkName,
            MobileSwiftModuleManager.platformName(sdk!!.platform),
            architectureDir.name,
            sdk.homePath,
            Objects.requireNonNull<Version>(Version.parseVersion(versionDir.name)),
            sdk.variant,
            sdk.versionString
        )
    }
}