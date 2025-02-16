/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.dwarf.entries

/** https://dwarfstd.org/doc/DWARF5.pdf **/
enum class DwTag(val value: UInt) {
    ARRAY_TYPE(0x01u),
    CLASS_TYPE(0x02u),
    ENTRY_POINT(0x03u),
    ENUMERATION_TYPE(0x04u),
    FORMAL_PARAMETER(0x05u),
    /** Reserved 0x06 **/
    /** Reserved 0x07 **/
    IMPORTED_DECLARATION(0x08u),
    /** Reserved 0x09 **/
    LABEL(0x0au),
    LEXICAL_BLOCK(0x0bu),
    /** Reserved 0x0c **/
    MEMBER(0x0du),
    /** Reserved 0x0e **/
    POINTER_TYPE(0x0fu),
    REFERENCE_TYPE(0x10u),
    COMPILE_UNIT(0x11u),
    STRING_TYPE(0x12u),
    STRUCTURE_TYPE(0x13u),
    /** Reserved 0x14 **/
    SUBROUTINE_TYPE(0x15u),
    TYPEDEF(0x16u),
    UNION_TYPE(0x17u),
    UNSPECIFIED_PARAMETERS(0x18u),
    VARIANT(0x19u),
    COMMON_BLOCK(0x1au),
    COMMON_INCLUSION(0x1bu),
    INHERITANCE(0x1cu),
    INLINED_SUBROUTINE(0x1du),
    MODULE(0x1eu),
    PTR_TO_MEMBER_TYPE(0x1fu),
    SET_TYPE(0x20u),
    SUBRANGE_TYPE(0x21u),
    WITH_STMT(0x22u),
    ACCESS_DECLARATION(0x23u),
    BASE_TYPE(0x24u),
    CATCH_BLOCK(0x25u),
    CONST_TYPE(0x26u),
    CONSTANT(0x27u),
    ENUMERATOR(0x28u),
    FILE_TYPE(0x29u),
    FRIEND(0x2au),
    NAMELIST(0x2bu),
    NAMELIST_ITEM(0x2cu),
    PACKED_TYPE(0x2du),
    SUBPROGRAM(0x2eu),
    TEMPLATE_TYPE_PARAMETER(0x2fu),
    TEMPLATE_VALUE_PARAMETER(0x30u),
    THROWN_TYPE(0x31u),
    TRY_BLOCK(0x32u),
    VARIANT_PART(0x33u),
    VARIABLE(0x34u),
    VOLATILE_TYPE(0x35u),
    DWARF_PROCEDURE(0x36u),
    RESTRICT_TYPE(0x37u),
    INTERFACE_TYPE(0x38u),
    NAMESPACE(0x39u),
    IMPORTED_MODULE(0x3au),
    UNSPECIFIED_TYPE(0x3bu),
    PARTIAL_UNIT(0x3cu),
    IMPORTED_UNIT(0x3du),
    /** Reserved 0x3e1 **/
    CONDITION(0x3fu),
    SHARED_TYPE(0x40u),
    TYPE_UNIT(0x41u),
    RVALUE_REFERENCE_TYPE(0x42u),
    TEMPLATE_ALIAS(0x43u),
    COARRAY_TYPE(0x44u),
    GENERIC_SUBRANGE(0x45u),
    DYNAMIC_TYPE(0x46u),
    ATOMIC_TYPE(0x47u),
    CALL_SITE(0x48u),
    CALL_SITE_PARAMETER(0x49u),
    SKELETON_UNIT(0x4au),
    IMMUTABLE_TYPE(0x4bu),
    LO_USER(0x4080u),
    HI_USER(0xffffu),
}