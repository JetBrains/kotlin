// !WITH_BASIC_TYPES

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 1: When expression with bound value (the form where the expression enclosed in parantheses is present) are very similar to the form without bound value, but use different syntax for conditions.
 NUMBER: 3
 DESCRIPTION: 'When' with bound value and with invalid list of the conditions in 'when entry'.
 */

fun case_1(value: Int, value2: _BasicTypesProvider): String {
    when (value) {
        -10000, value2.getInt(11), Int.MIN_VALUE, <!SYNTAX!><!>-> return ""
        21, <!SYNTAX!>,<!> <!SYNTAX!><!>-> return ""
        <!SYNTAX!>,<!> <!SYNTAX!>,<!> <!SYNTAX!><!>-> return ""
        <!SYNTAX!>,<!> value2.getInt(11) -> return ""
        value2.getInt(11) <!UNRESOLVED_REFERENCE!>Int<!><!SYNTAX!>.<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>MIN_VALUE<!><!SYNTAX!><!> -> return ""
        value2.getInt(11) <!SYNTAX!>200<!><!SYNTAX!><!> <!SYNTAX!><!>-> return ""
    }

    return ""
}
