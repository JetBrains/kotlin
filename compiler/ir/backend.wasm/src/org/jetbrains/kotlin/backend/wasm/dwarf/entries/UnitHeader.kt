/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

/** https://dwarfstd.org/doc/DWARF5.pdf, Page 199 **/
enum class UnitHeader(val opcode: UByte) {
    COMPILE(0x01u),
    TYPE(0x02u),
    PARTIAL(0x03u),
    SKELETON(0x04u),
    SPLIT_COMPILE(0x05u),
    SPLIT_TYPE(0x06u),
    LO_USER(0x80u),
    HI_USER(0xffu),
}