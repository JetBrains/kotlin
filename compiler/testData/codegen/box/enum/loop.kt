/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()


enum class Zzz {
    Z {
        init {
            sb.appendLine(Z.name)
        }
    }
}

fun box(): String {
    sb.appendLine(Zzz.Z)
    assertEquals("""
        Z
        Z

    """.trimIndent(), sb.toString())
    return "OK"
}