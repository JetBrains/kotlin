// !WITH_ENUM_CLASSES
// !WITH_SEALED_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 11
 SENTENCE: [1] It has an else entry;
 NUMBER: 3
 DESCRIPTION: Check when exhaustive via else entry (when with bound value, redundant else).
 */

// CASE DESCRIPTION: Checking for redundant 'else' branch (all enum values covered).
fun case_1(value: _EnumClass): String = when (value) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all enum values and null value covered).
fun case_2(value: _EnumClass?): String = when (value) {
    _EnumClass.EAST -> ""
    _EnumClass.NORTH -> ""
    _EnumClass.SOUTH -> ""
    _EnumClass.WEST -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (both boolean value covered).
fun case_3(value: Boolean): String = when (value) {
    true -> ""
    false -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (both boolean value and null value covered).
fun case_4(value: Boolean?): String = when (value) {
    true -> ""
    false -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all sealed class subtypes covered).
fun case_5(value: _SealedClass): String = when (value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (all sealed class subtypes and null value covered).
fun case_6(value: _SealedClass?): String = when (value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (sealed class itself covered).
fun case_7(value: _SealedClassSingle): String = when (value) {
    <!USELESS_IS_CHECK!>is _SealedClassSingle<!> -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: Checking for redundant 'else' branch (sealed class itself and null value covered).
fun case_8(value: _SealedClassSingle?): String = when (value) {
    is _SealedClassSingle -> ""
    null -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}