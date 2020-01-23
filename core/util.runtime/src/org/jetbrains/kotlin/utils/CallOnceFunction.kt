/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.util.concurrent.atomic.AtomicReference

class CallOnceFunction<F, T>(private val defaultValue: T, delegate: Function1<F, T>) : Function1<F, T> {
    private val functionRef: AtomicReference<Function1<F, T>?> = AtomicReference(delegate)

    override fun invoke(p1: F): T = functionRef.getAndSet(null)?.invoke(p1) ?: defaultValue
}