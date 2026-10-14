/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

/**
 * This component helps to interrupt a potentially long-lasting Klib loading process if cancellation was requested by the calling party.
 *
 * The implementation of [KlibLoader] is guaranteed to call [checkCanceled] function periodically. So, if [checkCanceled] throws
 * an exception, this naturally interrupts the ongoing Klib loading process.
 */
fun interface KlibLoadingCancellationChecker {
    fun checkCanceled()
}
