/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

abstract class JsPlatform : SimplePlatform("JS") {
    override val oldFashionedDescription: String
        get() = "JavaScript "
}

// TODO: temporarily conservative implementation; use the same approach as for TargetPlatform?.isNative()
//  when JsPlatform will become parameterized with "JS target"
fun TargetPlatform?.isJs(): Boolean = this?.singleOrNull() is JsPlatform