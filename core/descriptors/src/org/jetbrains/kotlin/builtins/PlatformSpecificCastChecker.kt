/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.container.DefaultImplementation
import org.jetbrains.kotlin.types.KotlinType

@DefaultImplementation(impl = PlatformSpecificCastChecker.Default::class)
interface PlatformSpecificCastChecker {
    fun isCastPossible(fromType: KotlinType, toType: KotlinType): Boolean

    class Default : PlatformSpecificCastChecker {
        override fun isCastPossible(fromType: KotlinType, toType: KotlinType): Boolean {
            return false
        }
    }
}
