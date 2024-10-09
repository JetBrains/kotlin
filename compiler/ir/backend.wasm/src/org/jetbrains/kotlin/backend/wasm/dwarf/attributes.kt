/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

/** https://dwarfstd.org/doc/DWARF5.pdf **/
enum class DwAttribute(val value: Int) {
    SIBLING(0x01),
    LOCATION(0x02),
    NAME(0x03),
    /** Reserved 0x04 **/
    /** Reserved 0x05 **/
    /** Reserved 0x06 **/
    /** Reserved 0x07 **/
    /** Reserved 0x08 **/
    ORDERING(0x09),
    /** Reserved 0x0a **/
    BYTE_SIZE(0x0b),
    /** Reserved 0x0c2 **/
    BIT_SIZE(0x0d),
    /** Reserved 0x0e **/
    /** Reserved 0x0f **/
    STMT_LIST(0x10),
    LOW_PC(0x11),
    HIGH_PC(0x12),
    LANGUAGE(0x13),
    /** Reserved 0x14 **/
    DISCR(0x15),
    DISCR_VALUE(0x16),
    VISIBILITY(0x17),
    IMPORT(0x18),
    STRING_LENGTH(0x19),
    COMMON_REFERENCE(0x1a),
    COMP_DIR(0x1b),
    CONST_VALUE(0x1c),
    CONTAINING_TYPE(0x1d),
    DEFAULT_VALUE(0x1e),
    /** Reserved 0x1f **/
    INLINE(0x20),
    IS_OPTIONAL(0x21),
    LOWER_BOUND(0x22),
    /** Reserved 0x23 **/
    /** Reserved 0x24 **/
    PRODUCER(0x25),
    /** Reserved 0x26 **/
    PROTOTYPED(0x27),
    /** Reserved 0x28 **/
    /** Reserved 0x29 **/
    RETURN_ADDR(0x2a),
    /** Reserved 0x2b **/
    START_SCOPE(0x2c),
    /** Reserved 0x2d **/
    BIT_STRIDE(0x2e),
    UPPER_BOUND(0x2f),
    /** Reserved 0x30 **/
    ABSTRACT_ORIGIN(0x31),
    ACCESSIBILITY(0x32),
    ADDRESS_CLASS(0x33),
    ARTIFICIAL(0x34),
    BASE_TYPES(0x35),
    CALLING_CONVENTION(0x36),
    COUNT(0x37),
    DATA_MEMBER_LOCATION(0x38),
    DECL_COLUMN(0x39),
    DECL_FILE(0x3a),
    DECL_LINE(0x3b),
    DECLARATION(0x3c),
    DISCR_LIST(0x3d),
    ENCODING(0x3e),
    EXTERNAL(0x3f),
    FRAME_BASE(0x40),
    FRIEND(0x41),
    IDENTIFIER_CASE(0x42),
    /** Reserved 0x433 **/
    NAMELIST_ITEM(0x44),
    PRIORITY(0x45),
    SEGMENT(0x46),
    SPECIFICATION(0x47),
    STATIC_LINK(0x48),
    TYPE(0x49),
    USE_LOCATION(0x4a),
    VARIABLE_PARAMETER(0x4b),
    VIRTUALITY(0x4c),
    VTABLE_ELEM_LOCATION(0x4d),
    ALLOCATED(0x4e),
    ASSOCIATED(0x4f),
    DATA_LOCATION(0x50),
    BYTE_STRIDE(0x51),
    ENTRY_PC(0x52),
    USE_UTF8(0x53),
    EXTENSION(0x54),
    RANGES(0x55),
    TRAMPOLINE(0x56),
    CALL_COLUMN(0x57),
    CALL_FILE(0x58),
    CALL_LINE(0x59),
    DESCRIPTION(0x5a),
    BINARY_SCALE(0x5b),
    DECIMAL_SCALE(0x5c),
    SMALL(0x5d),
    DECIMAL_SIGN(0x5e),
    DIGIT_COUNT(0x5f),
    PICTURE_STRING(0x60),
    MUTABLE(0x61),
    THREADS_SCALED(0x62),
    EXPLICIT(0x63),
    OBJECT_POINTER(0x64),
    ENDIANITY(0x65),
    ELEMENTAL(0x66),
    PURE(0x67),
    RECURSIVE(0x68),
    SIGNATURE(0x69),
    MAIN_SUBPROGRAM(0x6a),
    DATA_BIT_OFFSET(0x6b),
    CONST_EXPR(0x6c),
    ENUM_CLASS(0x6d),
    LINKAGE_NAME(0x6e),
    STRING_LENGTH_BIT_SIZE(0x6f),
    STRING_LENGTH_BYTE_SIZE(0x70),
    RANK(0x71),
    STR_OFFSETS_BASE(0x72),
    ADDR_BASE(0x73),
    RNGLISTS_BASE(0x74),
    /** Reserved 0x75 **/
    DWO_NAME(0x76),
    REFERENCE(0x77),
    RVALUE_REFERENCE(0x78),
    MACROS(0x79),
    CALL_ALL_CALLS(0x7a),
    CALL_ALL_SOURCE_CALLS(0x7b),
    CALL_ALL_TAIL_CALLS(0x7c),
    CALL_RETURN_PC(0x7d),
    CALL_VALUE(0x7e),
    CALL_ORIGIN(0x7f),
    CALL_PARAMETER(0x80),
    CALL_PC(0x81),
    CALL_TAIL_CALL(0x82),
    CALL_TARGET(0x83),
    CALL_TARGET_CLOBBERED(0x84),
    CALL_DATA_LOCATION(0x85),
    CALL_DATA_VALUE(0x86),
    NORETURN(0x87),
    ALIGNMENT(0x88),
    EXPORT_SYMBOLS(0x89),
    DELETED(0x8a),
    DEFAULTED(0x8b),
    LOCLISTS_BASE(0x8c),
    LO_USER(0x2000),
    HI_USER(0x3fff),
}