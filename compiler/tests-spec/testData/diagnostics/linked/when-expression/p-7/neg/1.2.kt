// !WITH_CLASSES

/*
 KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)

 SECTION: when-expression
 PARAGRAPH: 7
 SENTENCE: [1] Type test condition: type checking operator followed by type.
 NUMBER: 2
 DESCRIPTION: 'When' with bound value and type test condition on the non-type operand of the type checking operator.
 */

fun case_1(value: Any, <!UNUSED_PARAMETER!>value1<!>: Int): String {
    when (value) {
        is <!UNRESOLVED_REFERENCE!>value1<!> -> return ""
    }

    return ""
}
