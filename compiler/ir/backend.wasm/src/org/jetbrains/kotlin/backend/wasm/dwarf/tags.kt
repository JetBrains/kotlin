/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf

/** https://dwarfstd.org/doc/DWARF5.pdf **/
enum class DwTag(val value: Int) {
    ARRAY_TYPE(0x01),
    CLASS_TYPE(0x02),
    ENTRY_POINT(0x03),
    ENUMERATION_TYPE(0x04),
    FORMAL_PARAMETER(0x05),
    /** Reserved 0x06 **/
    /** Reserved 0x07 **/
    IMPORTED_DECLARATION(0x08),
    /** Reserved 0x09 **/
    LABEL(0x0a),
    LEXICAL_BLOCK(0x0b),
    /** Reserved 0x0c **/
    MEMBER(0x0d),
    /** Reserved 0x0e **/
    POINTER_TYPE(0x0f),
    REFERENCE_TYPE(0x10),
    COMPILE_UNIT(0x11),
    STRING_TYPE(0x12),
    STRUCTURE_TYPE(0x13),
    /** Reserved 0x14 **/
    SUBROUTINE_TYPE(0x15),
    TYPEDEF(0x16),
    UNION_TYPE(0x17),
    UNSPECIFIED_PARAMETERS(0x18),
    VARIANT(0x19),
    COMMON_BLOCK(0x1a),
    COMMON_INCLUSION(0x1b),
    INHERITANCE(0x1c),
    INLINED_SUBROUTINE(0x1d),
    MODULE(0x1e,),
    PTR_TO_MEMBER_TYPE(0x1f),
    SET_TYPE(0x20),
    SUBRANGE_TYPE(0x21),
    WITH_STMT(0x22),
    ACCESS_DECLARATION(0x23),
    BASE_TYPE(0x24),
    CATCH_BLOCK(0x25),
    CONST_TYPE(0x26),
    CONSTANT(0x27),
    ENUMERATOR(0x28),
    FILE_TYPE(0x29),
    FRIEND(0x2a),
    NAMELIST(0x2b),
    NAMELIST_ITEM(0x2c),
    PACKED_TYPE(0x2d),
    SUBPROGRAM(0x2e),
    TEMPLATE_TYPE_PARAMETER(0x2f),
    TEMPLATE_VALUE_PARAMETER(0x30),
    THROWN_TYPE(0x31),
    TRY_BLOCK(0x32),
    VARIANT_PART(0x33),
    VARIABLE(0x34),
    VOLATILE_TYPE(0x35),
    DWARF_PROCEDURE(0x36),
    RESTRICT_TYPE(0x37),
    INTERFACE_TYPE(0x38),
    NAMESPACE(0x39),
    IMPORTED_MODULE(0x3a),
    UNSPECIFIED_TYPE(0x3b),
    PARTIAL_UNIT(0x3c),
    IMPORTED_UNIT(0x3d),
    /** Reserved 0x3e1 **/
    CONDITION(0x3f),
    SHARED_TYPE(0x40),
    TYPE_UNIT(0x41),
    RVALUE_REFERENCE_TYPE(0x42),
    TEMPLATE_ALIAS(0x43),
    COARRAY_TYPE(0x44),
    GENERIC_SUBRANGE(0x45),
    DYNAMIC_TYPE(0x46),
    ATOMIC_TYPE(0x47),
    CALL_SITE(0x48),
    CALL_SITE_PARAMETER(0x49),
    SKELETON_UNIT(0x4a),
    IMMUTABLE_TYPE(0x4b),
    LO_USER(0x4080),
    HI_USER(0xffff),
}