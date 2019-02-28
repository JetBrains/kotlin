/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import kotlinx.coroutines.runBlocking

fun <R> Profiler.withMeasureBlocking(obj: Any?, body: suspend () -> R): R = runBlocking { withMeasure<R>(obj) { body() } }