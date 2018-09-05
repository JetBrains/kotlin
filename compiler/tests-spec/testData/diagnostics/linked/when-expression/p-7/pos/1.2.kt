// !WITH_SEALED_CLASSES
// !WITH_CLASSES
// !WITH_OBJECTS

/*
 KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and type test condition (invert type checking operator).
 */

// CASE DESCRIPTION: 'When' in which all branches includes invert type checking operators.
fun case_1(value: _SealedClass) = when (value) {
    !is _SealedChild1 -> {}
    !is _SealedChild2 -> {}
    !is _SealedChild3 -> {}
}

/*
 CASE DESCRIPTION: 'When' with direct and invert (with null-check) type checking operators on the same types and redundant null-check.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_2(value: _SealedClass?): String = when (value) {
    !is _SealedChild2 -> "" // including null
    <!USELESS_IS_CHECK!>is _SealedChild2<!> -> ""
    null -> "" // redundant
}

// CASE DESCRIPTION: 'When' with direct and invert type checking operators on the same types and null-check.
fun case_3(value: _SealedClass?): String = when (value) {
    !is _SealedChild2? -> "" // null isn't included
    is _SealedChild2 -> ""
    null -> ""
}

/*
 CASE DESCRIPTION: 'When' with direct and invert (with null-check) type checking operators on the same types.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-22996
 */
fun case_4(value: _SealedClass?) {
    when (value) {
        !is _SealedChild2 -> {} // including null
        <!USELESS_IS_CHECK!>is _SealedChild2?<!> -> {} // redundant nullable type check
    }
}

// CASE DESCRIPTION: 'When' with direct and invert type checking operator on the objects.
fun case_5(value: Any): String {
    when (value) {
        is _EmptyObject -> return ""
        !is _ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}
