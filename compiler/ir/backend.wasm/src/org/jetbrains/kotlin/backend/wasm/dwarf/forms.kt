/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

enum class DwForm(val opcode: UInt) {
    ADDR(0x01u),
    // Reserved 0x02
    BLOCK2(0x03u),
    BLOCK4(0x04u),
    DATA2(0x05u),
    DATA4(0x06u),
    DATA8(0x07u),
    STRING(0x08u),
    BLOCK(0x09u),
    BLOCK1(0x0au),
    DATA1(0x0bu),
    FLAG(0x0cu),
    SDATA(0x0du),
    STRP(0x0eu),
    UDATA(0x0fu),
    REF_ADDR(0x10u),
    REF1(0x11u),
    REF2(0x12u),
    REF4(0x13u),
    REF8(0x14u),
    REF_UDATA(0x15u),
    INDIRECT(0x16u),
    SEC_OFFSET(0x17u),
    EXPRLOC(0x18u),
    FLAG_PRESENT(0x19u),
    STRX(0x1au),
    ADDRX(0x1bu),
    REF_SUP4(0x1cu),
    STRP_SUP(0x1du),
    DATA16(0x1eu),
    LINE_STRP(0x1fu),
    REF_SIG8(0x20u),
    IMPLICIT_CONST(0x21u),
    LOCLISTX(0x22u),
    RNGLISTX(0x23u),
    REF_SUP8(0x24u),
    STRX1(0x25u),
    STRX2(0x26u),
    STRX3(0x27u),
    STRX4(0x28u),
    ADDRX1(0x29u),
    ADDRX2(0x2au),
    ADDRX3(0x2bu),
    ADDRX4(0x2cu),
}