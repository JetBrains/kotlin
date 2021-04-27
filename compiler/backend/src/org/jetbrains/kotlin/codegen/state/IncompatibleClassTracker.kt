/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass

interface IncompatibleClassTracker {
    fun record(binaryClass: KotlinJvmBinaryClass)

    object DoNothing : IncompatibleClassTracker {
        override fun record(binaryClass: KotlinJvmBinaryClass) {
        }
    }
}