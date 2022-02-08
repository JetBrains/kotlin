/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass

// Used in ru.yole.jitwatch-intellij:1.0
@Deprecated("Deprecated and left for binary compatibility with third-party IntelliJ IDEA plugins.", level = DeprecationLevel.ERROR)
interface IncompatibleClassTracker {
    fun record(binaryClass: KotlinJvmBinaryClass)

    @Suppress("DEPRECATION_ERROR")
    @Deprecated("Deprecated and left for binary compatibility with third-party IntelliJ IDEA plugins.", level = DeprecationLevel.ERROR)
    object DoNothing : IncompatibleClassTracker {
        override fun record(binaryClass: KotlinJvmBinaryClass) {
        }
    }
}