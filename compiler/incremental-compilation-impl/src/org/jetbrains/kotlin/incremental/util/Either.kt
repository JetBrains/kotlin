/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.util

sealed class Either<out T> {
    class Success<T>(val value: T) : Either<T>()
    class Error(val reason: String) : Either<Nothing>()
}