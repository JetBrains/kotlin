/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

object WasmStructLayout {
    const val VTABLE_FIELD_INDEX = 0    // GC reference
    const val ITABLE_FIELD_INDEX = 1    // GC reference
    const val TYPE_ID_FIELD_INDEX = 2   // i32
    const val HASH_CODE_FIELD_INDEX = 3 // i32

    const val NUMBER_OF_IMPLICIT_FIELDS = 4
}