/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

/** https://dwarfstd.org/doc/DWARF5.pdf **/
enum class DwAttribute(val opcode: UInt) {
    SIBLING(0x01u),
    LOCATION(0x02u),
    NAME(0x03u),
    /** Reserved 0x04 **/
    /** Reserved 0x05 **/
    /** Reserved 0x06 **/
    /** Reserved 0x07 **/
    /** Reserved 0x08 **/
    ORDERING(0x09u),
    /** Reserved 0x0a **/
    BYTE_SIZE(0x0bu),
    /** Reserved 0x0c2 **/
    BIT_SIZE(0x0du),
    /** Reserved 0x0e **/
    /** Reserved 0x0f **/
    STMT_LIST(0x10u),
    LOW_PC(0x11u),
    HIGH_PC(0x12u),
    LANGUAGE(0x13u),
    /** Reserved 0x14 **/
    DISCR(0x15u),
    DISCR_VALUE(0x16u),
    VISIBILITY(0x17u),
    IMPORT(0x18u),
    STRING_LENGTH(0x19u),
    COMMON_REFERENCE(0x1au),
    COMP_DIR(0x1bu),
    CONST_VALUE(0x1cu),
    CONTAINING_TYPE(0x1du),
    DEFAULT_VALUE(0x1eu),
    /** Reserved 0x1f **/
    INLINE(0x20u),
    IS_OPTIONAL(0x21u),
    LOWER_BOUND(0x22u),
    /** Reserved 0x23 **/
    /** Reserved 0x24 **/
    PRODUCER(0x25u),
    /** Reserved 0x26 **/
    PROTOTYPED(0x27u),
    /** Reserved 0x28 **/
    /** Reserved 0x29 **/
    RETURN_ADDR(0x2au),
    /** Reserved 0x2b **/
    START_SCOPE(0x2cu),
    /** Reserved 0x2d **/
    BIT_STRIDE(0x2eu),
    UPPER_BOUND(0x2fu),
    /** Reserved 0x30 **/
    ABSTRACT_ORIGIN(0x31u),
    ACCESSIBILITY(0x32u),
    ADDRESS_CLASS(0x33u),
    ARTIFICIAL(0x34u),
    BASE_TYPES(0x35u),
    CALLING_CONVENTION(0x36u),
    COUNT(0x37u),
    DATA_MEMBER_LOCATION(0x38u),
    DECL_COLUMN(0x39u),
    DECL_FILE(0x3au),
    DECL_LINE(0x3bu),
    DECLARATION(0x3cu),
    DISCR_LIST(0x3du),
    ENCODING(0x3eu),
    EXTERNAL(0x3fu),
    FRAME_BASE(0x40u),
    FRIEND(0x41u),
    IDENTIFIER_CASE(0x42u),
    /** Reserved 0x433 **/
    NAMELIST_ITEM(0x44u),
    PRIORITY(0x45u),
    SEGMENT(0x46u),
    SPECIFICATION(0x47u),
    STATIC_LINK(0x48u),
    TYPE(0x49u),
    USE_LOCATION(0x4au),
    VARIABLE_PARAMETER(0x4bu),
    VIRTUALITY(0x4cu),
    VTABLE_ELEM_LOCATION(0x4du),
    ALLOCATED(0x4eu),
    ASSOCIATED(0x4fu),
    DATA_LOCATION(0x50u),
    BYTE_STRIDE(0x51u),
    ENTRY_PC(0x52u),
    USE_UTF8(0x53u),
    EXTENSION(0x54u),
    RANGES(0x55u),
    TRAMPOLINE(0x56u),
    CALL_COLUMN(0x57u),
    CALL_FILE(0x58u),
    CALL_LINE(0x59u),
    DESCRIPTION(0x5au),
    BINARY_SCALE(0x5bu),
    DECIMAL_SCALE(0x5cu),
    SMALL(0x5du),
    DECIMAL_SIGN(0x5eu),
    DIGIT_COUNT(0x5fu),
    PICTURE_STRING(0x60u),
    MUTABLE(0x61u),
    THREADS_SCALED(0x62u),
    EXPLICIT(0x63u),
    OBJECT_POINTER(0x64u),
    ENDIANITY(0x65u),
    ELEMENTAL(0x66u),
    PURE(0x67u),
    RECURSIVE(0x68u),
    SIGNATURE(0x69u),
    MAIN_SUBPROGRAM(0x6au),
    DATA_BIT_OFFSET(0x6bu),
    CONST_EXPR(0x6cu),
    ENUM_CLASS(0x6du),
    LINKAGE_NAME(0x6eu),
    STRING_LENGTH_BIT_SIZE(0x6fu),
    STRING_LENGTH_BYTE_SIZE(0x70u),
    RANK(0x71u),
    STR_OFFSETS_BASE(0x72u),
    ADDR_BASE(0x73u),
    RNGLISTS_BASE(0x74u),
    /** Reserved 0x75 **/
    DWO_NAME(0x76u),
    REFERENCE(0x77u),
    RVALUE_REFERENCE(0x78u),
    MACROS(0x79u),
    CALL_ALL_CALLS(0x7au),
    CALL_ALL_SOURCE_CALLS(0x7bu),
    CALL_ALL_TAIL_CALLS(0x7cu),
    CALL_RETURN_PC(0x7du),
    CALL_VALUE(0x7eu),
    CALL_ORIGIN(0x7fu),
    CALL_PARAMETER(0x80u),
    CALL_PC(0x81u),
    CALL_TAIL_CALL(0x82u),
    CALL_TARGET(0x83u),
    CALL_TARGET_CLOBBERED(0x84u),
    CALL_DATA_LOCATION(0x85u),
    CALL_DATA_VALUE(0x86u),
    NORETURN(0x87u),
    ALIGNMENT(0x88u),
    EXPORT_SYMBOLS(0x89u),
    DELETED(0x8au),
    DEFAULTED(0x8bu),
    LOCLISTS_BASE(0x8cu),
    LO_USER(0x2000u),
    HI_USER(0x3fffu),
}