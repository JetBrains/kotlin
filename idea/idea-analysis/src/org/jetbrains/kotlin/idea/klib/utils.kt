/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.klib

import org.jetbrains.kotlin.library.KotlinLibrary
import java.io.IOException

fun <T> KotlinLibrary.readSafe(defaultValue: T, action: KotlinLibrary.() -> T) = try {
    action()
} catch (_: IOException) {
    defaultValue
}
