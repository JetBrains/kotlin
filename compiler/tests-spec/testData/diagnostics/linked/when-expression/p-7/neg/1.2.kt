// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTIONS: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and type test condition on the non-type operand of the type checking operator.
 */

fun case_1(value_1: Any, <!UNUSED_PARAMETER!>value_2<!>: Int): String {
    when (value_1) {
        is <!UNRESOLVED_REFERENCE!>value_2<!> -> return ""
    }

    return ""
}
