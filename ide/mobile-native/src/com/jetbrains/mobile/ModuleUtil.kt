package com.jetbrains.mobile

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform

private fun Module.targetPlatforms(): Set<SimplePlatform> {
    val facets = FacetManager.getInstance(this).allFacets
    if (facets.size > 1) throw IllegalStateException()
    val facetConfiguration = facets.singleOrNull()?.configuration as? KotlinFacetConfiguration
    return facetConfiguration?.settings?.targetPlatform?.componentPlatforms ?: emptySet()
}

private fun Module.targetPlatformOrNull(): SimplePlatform? = targetPlatforms().singleOrNull()

val Module.isCommon: Boolean
    get() {
        val platforms = targetPlatforms()
        return platforms.any { it is JdkPlatform } && platforms.any { it is NativePlatform }
    }

val Module.isCommonMain: Boolean
    get() = name.endsWith("Main") && isCommon

val Module.isCommonTest: Boolean
    get() = name.endsWith("Test") && isCommon

val Module.isAndroid: Boolean
    get() = targetPlatformOrNull() is JdkPlatform

val Module.isApple: Boolean
    get() = targetPlatformOrNull() is NativePlatform

val Module.isMobileAppMain: Boolean
    get() = name.endsWith("Main") && (isAndroid || isApple)

val Module.isMobileAppTest: Boolean
    get() = name.endsWith("Test") && (isAndroid || isApple) // FIXME