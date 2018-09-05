// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 1
 DESCRIPTION: 'When' with bound value and type test condition (without companion object in classes), but without type checking operator.
 */

// CASE DESCRIPTION: 'When' with custom class type test condition.
fun case_1(value: Any): String {
    when (value) {
        <!NO_COMPANION_OBJECT!>_EmptyClass<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with Any type test condition.
fun case_2(value: Any): String {
    when (value) {
        <!NO_COMPANION_OBJECT!>Any<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with Nothing type test condition.
fun case_3(value: Any): String {
    when (value) {
        <!NO_COMPANION_OBJECT!>Nothing<!> -> return ""
    }

    return ""
}
