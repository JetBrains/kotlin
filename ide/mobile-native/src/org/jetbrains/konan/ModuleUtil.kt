/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.facet.FacetManager
import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.facet.KotlinFacetConfiguration
import org.jetbrains.kotlin.platform.SimplePlatform
import org.jetbrains.kotlin.platform.jvm.JdkPlatform
import org.jetbrains.kotlin.platform.konan.KonanPlatform

private fun Module.targetPlatformOrNull(): SimplePlatform? {
    val facets = FacetManager.getInstance(this).allFacets
    if (facets.size > 1) throw IllegalStateException()
    val facetConfiguration = facets.singleOrNull()?.configuration as? KotlinFacetConfiguration
    return facetConfiguration?.settings?.targetPlatform?.componentPlatforms?.singleOrNull()
}

val Module.isAndroid: Boolean
    get() = targetPlatformOrNull() is JdkPlatform

val Module.isApple: Boolean
    get() = targetPlatformOrNull() is KonanPlatform