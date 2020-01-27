/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.signature

enum class IdSignatureFlags {
    IS_EXPECT,
    IS_JAVA_FOR_KOTLIN_OVERRIDE_PPROPERTY;

    fun encode(isSet: Boolean): Long = if (isSet) 1L shl ordinal else 0L
    fun decode(flags: Long): Boolean = (flags and (1L shl ordinal) != 0L)
}
