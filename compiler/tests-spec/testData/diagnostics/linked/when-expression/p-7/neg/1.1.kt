// !WITH_CLASSES

/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: when-expression
 * PARAGRAPH: 7
 * SENTENCE: [1] Type test condition: type checking operator followed by type.
 * NUMBER: 1
 * DESCRIPTION: 'When' with bound value and type test condition (without companion object in classes), but without type checking operator.
 */

// TESTCASE NUMBER: 1
fun case_1(value_1: Any): String {
    when (value_1) {
        <!NO_COMPANION_OBJECT!>_EmptyClass<!> -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Any): String {
    when (value_1) {
        <!NO_COMPANION_OBJECT!>Any<!> -> return ""
    }

    return ""
}

// TESTCASE NUMBER: 3
fun case_3(value_1: Any): String {
    when (value_1) {
        <!NO_COMPANION_OBJECT!>Nothing<!> -> return ""
    }

    return ""
}
