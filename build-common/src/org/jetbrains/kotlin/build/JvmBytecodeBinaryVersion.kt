/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

/**
 * Bytecode version was in the Kotlin metadata ([Metadata.bytecodeVersion]) since 1.0, but it was never used meaningfully in the compiler,
 * outside of one very special case regarding experimental coroutines, which is now obsolete. It is still used in incremental compilation
 * caches though. We should probably just pretend that the bytecode version of any Kotlin file from now on is equal to the latest observed
 * bytecode version, `1.0.3` (see [INSTANCE]).
 */
class JvmBytecodeBinaryVersion(vararg numbers: Int) {
    val major: Int = numbers.getOrNull(0) ?: -1
    val minor: Int = numbers.getOrNull(1) ?: -1
    val patch: Int = numbers.getOrNull(2) ?: -1

    override fun toString(): String = buildString {
        append(major)
        if (minor != -1) {
            append(".$minor")
            if (patch != -1) append(".$patch")
        }
    }

    companion object {
        @JvmField
        val INSTANCE = JvmBytecodeBinaryVersion(1, 0, 3)
    }
}
