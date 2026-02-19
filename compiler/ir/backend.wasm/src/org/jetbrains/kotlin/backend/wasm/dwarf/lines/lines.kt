/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.lines

/** https://dwarfstd.org/doc/DWARF5.pdf, Page 236 **/
enum class DwLines(val opcode: UInt) {
    COPY(0x01u),
    ADVANCE_PC(0x02u),
    ADVANCE_LINE(0x03u),
    SET_FILE(0x04u),
    SET_COLUMN(0x05u),
    NEGATE_STMT(0x06u),
    SET_BASIC_BLOCK(0x07u),
    CONST_ADD_PC(0x08u),
    FIXED_ADVANCE_PC(0x09u),
    SET_PROLOGUE_END(0x0au),
    SET_EPILOGUE_BEGIN(0x0bu),
    SET_ISA(0x0cu),

    // Extended
    END_SEQUENCE(0x01u),
    SET_ADDRESS(0x02u)
}

/** https://dwarfstd.org/doc/DWARF5.pdf, Page 237 **/
enum class DwLinesHeader(val opcode: UInt) {
    PATH(0x01u),
    DIRECTORY_INDEX(0x02u),
    TIMESTAMP(0x03u),
    SIZE(0x04u),
    MD5(0x05u),
    LO_USER(0x2000u),
    HI_USER(0x3fffu),
}
