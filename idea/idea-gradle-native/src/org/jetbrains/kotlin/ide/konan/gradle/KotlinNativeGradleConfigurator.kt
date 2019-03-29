/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import org.jetbrains.kotlin.ide.konan.hasKotlinNativeRuntimeInScope
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinStatus
import org.jetbrains.kotlin.idea.configuration.KotlinWithGradleConfigurator
import org.jetbrains.kotlin.idea.configuration.ModuleSourceRootGroup
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms


open class KotlinNativeGradleConfigurator : KotlinWithGradleConfigurator() {
    override fun getKotlinPluginExpression(forKotlinDsl: Boolean): String {
        return ""
    }

    override fun getStatus(moduleSourceRootGroup: ModuleSourceRootGroup): ConfigureKotlinStatus {
        if (moduleSourceRootGroup.sourceRootModules.any(::hasKotlinNativeRuntimeInScope)) {
            return ConfigureKotlinStatus.CONFIGURED
        }
        return ConfigureKotlinStatus.NON_APPLICABLE
    }

    override val kotlinPluginName: String
        get() = ""

    override val name: String get() = NAME

    override val targetPlatform get() = DefaultBuiltInPlatforms.konanPlatform

    override val presentableText get() = PRESENTABLE_TEXT

    companion object {
        const val NAME = "KotlinNative"
        const val PRESENTABLE_TEXT = "Native"
    }
}