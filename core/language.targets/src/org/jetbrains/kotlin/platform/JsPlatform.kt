/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

abstract class JsPlatform : SimplePlatform("JS"), PotentiallyWebPlatform {
    override val oldFashionedDescription: String
        get() = "JavaScript "

    override val isWeb: Boolean
        get() = true
}

// TODO: temporarily conservative implementation; use the same approach as for TargetPlatform?.isNative()
//  when JsPlatform will become parameterized with "JS target"
fun TargetPlatform?.isJs(): Boolean = this?.singleOrNull() is JsPlatform