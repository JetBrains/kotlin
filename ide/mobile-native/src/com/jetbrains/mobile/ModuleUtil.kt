package com.jetbrains.mobile

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform

private fun Module.targetPlatformOrNull(): SimplePlatform? {
    val facets = FacetManager.getInstance(this).allFacets
    if (facets.size > 1) throw IllegalStateException()
    val facetConfiguration = facets.singleOrNull()?.configuration as? KotlinFacetConfiguration
    return facetConfiguration?.settings?.targetPlatform?.componentPlatforms?.singleOrNull()
}

val Module.isAndroid: Boolean
    get() = targetPlatformOrNull() is JdkPlatform

val Module.isApple: Boolean
    get() = targetPlatformOrNull() is NativePlatform

val Module.isMobileAppMain: Boolean
    get() = name.endsWith("Main") && (isAndroid || isApple)

val Module.isMobileAppTest: Boolean
    get() = name.endsWith("Test") && (isAndroid || isApple) // FIXME