// !DIAGNOSTICS: -UNUSED_EXPRESSION
// !WITH_SEALED_CLASSES

/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 3: Type test condition: type checking operator followed by type.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and type test condition (with sealed class).
 */

// CASE DESCRIPTION: 'When' with type test condition on the all possible subtypes of the sealed class.
fun case_1(value: _SealedClass): String = when (value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the not all possible subtypes of the sealed class.
fun case_2(value: _SealedClass): String {
    <!NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS!>when<!> (value) {
        is _SealedChild1 -> return ""
        is _SealedChild2 -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with type test condition on the not all possible subtypes of the sealed class and 'else' branch.
fun case_3(value: _SealedClass): String = when (value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    else -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the all possible subtypes of the sealed class and 'else' branch (redundant).
fun case_4(value: _SealedClass): String = when (value) {
    is _SealedChild1 -> ""
    is _SealedChild2 -> ""
    is _SealedChild3 -> ""
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

// CASE DESCRIPTION: 'When' with type test condition on the empty sealed class.
fun case_5(value: _SealedClassEmpty): String = when (value) {
    else -> ""
}
