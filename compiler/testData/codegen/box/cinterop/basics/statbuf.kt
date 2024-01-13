/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TARGET_BACKEND: NATIVE
// IGNORE_NATIVE: target=linux_arm64
// MODULE: cinterop
// FILE: sysstat.def
headers = sys/stat.h

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import sysstat.*
import kotlin.test.*
import kotlinx.cinterop.*

fun box(): String {
    val statBuf = nativeHeap.alloc<stat>()
    assertEquals(0, stat("/", statBuf.ptr))
    assertEquals("0", statBuf.st_uid.toString())

    return "OK"
}
