/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

/**
 * Bytecode version was in the Kotlin metadata ([Metadata.bytecodeVersion]) since 1.0, but it was never used meaningfully in the compiler,
 * outside of one very special case regarding experimental coroutines, which is now obsolete.
 * It is still used for two reasons:
 * 1) We write the latest observed bytecode version, `1.0.3` (see [INSTANCE]), to class files if metadata version is less than 1.5.
 *    The reason is that Kotlin compilers of versions 1.0.0-1.3.72 can still read these class files, and they had a strict check which
 *    results in a compilation error if there's no bytecode version in the `@Metadata` annotation (see KT-45323).
 *    This will not be needed at the moment when the earliest supported language version becomes 1.5, i.e. in Kotlin 1.7.
 * 2) It's stored in persistent incremental compilation caches. We can probably simply remove it and increase the cache version there.
 * Once both these usages are dealt with, this class can be removed.
 */
class JvmBytecodeBinaryVersion(vararg numbers: Int) {
    val major: Int = numbers.getOrNull(0) ?: -1
    val minor: Int = numbers.getOrNull(1) ?: -1
    val patch: Int = numbers.getOrNull(2) ?: -1

    fun toArray(): IntArray = intArrayOf(major, minor, patch)

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
