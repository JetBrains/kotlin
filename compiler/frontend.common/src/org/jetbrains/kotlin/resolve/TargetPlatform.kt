/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

@Deprecated(
    message = "This class is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
    replaceWith = ReplaceWith("TargetPlatform", "org.jetbrains.kotlin.platform.TargetPlatform"),
    level = DeprecationLevel.ERROR
)
interface TargetPlatform {
    val platformName: String
}
