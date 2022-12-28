/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata

/**
 * Possible values for [KlibMetadataProtoBuf.Header] flags field.
 */
object KlibMetadataHeaderFlags {
    // Note: previously the value of this flag was 0x1.
    const val PRE_RELEASE = 0x2
}
