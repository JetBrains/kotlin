/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.analyzer.common

import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

@Deprecated(
    message = "This class is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
    replaceWith = ReplaceWith("CommonPlatforms.defaultCommonPlatform", "org.jetbrains.kotlin.platform.CommonPlatforms"),
    level = DeprecationLevel.ERROR
)
interface CommonPlatform : TargetPlatform {
    @JvmDefault
    override val platformName: String
        get() = "Default"

    companion object {
        @JvmField
        val INSTANCE: CommonPlatform = CommonPlatforms.CompatCommonPlatform
    }
}
