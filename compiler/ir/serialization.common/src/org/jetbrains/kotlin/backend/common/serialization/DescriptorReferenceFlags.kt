/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization


enum class DescriptorReferenceFlags {
    IS_FAKE_OVERRIDE,
    IS_BACKING_FIELD,
    IS_GETTER,
    IS_SETTER,
    IS_DEFAULT_CONSTRUCTOR,
    IS_ENUM_ENTRY,
    IS_ENUM_SPECIAL,
    IS_TYPE_PARAMETER,
    IS_EXPECT;

    fun encode(isSet: Boolean): Int = if (isSet) 1 shl ordinal else 0
    fun decode(flags: Int): Boolean = (flags and (1 shl ordinal) != 0)
}
