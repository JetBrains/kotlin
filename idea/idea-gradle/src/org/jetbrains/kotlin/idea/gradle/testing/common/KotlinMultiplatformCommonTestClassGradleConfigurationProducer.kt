/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.testing.common

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.run.AbstractKotlinMultiplatformTestClassGradleConfigurationProducer
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon

class KotlinMultiplatformCommonTestClassGradleConfigurationProducer : AbstractKotlinMultiplatformTestClassGradleConfigurationProducer() {
    override fun isApplicable(module: Module, platform: TargetPlatform) = platform.isCommon()
}