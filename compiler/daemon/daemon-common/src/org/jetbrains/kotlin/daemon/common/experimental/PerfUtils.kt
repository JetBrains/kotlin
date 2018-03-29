/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.Profiler

fun<R> Profiler.withMeasureBlocking(obj: Any?, body: suspend () -> R): R = this.withMeasure(obj, {
    runBlocking { body() }
})