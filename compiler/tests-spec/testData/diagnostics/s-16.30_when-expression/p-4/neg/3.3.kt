// !WITH_CLASSES
// !WITH_OBJECTS

/*
 KOTLIN SPEC TEST (NEGATIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 4
 SENTENCE 3: Type test condition: type checking operator followed by type.
 NUMBER: 3
 DESCRIPTION: 'When' with bound value and 'when condition' with type checking operator and non-type value.
 */

// CASE DESCRIPTION: 'When' with custom object and companion object of class as type checking operator value.
fun case_1(value: Any): String {
    when (value) {
        is _EmptyObject -> return ""
        is _ClassWithCompanionObject.Companion -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with variables and return value as type checking operator value.
fun case_2(value: Any, <!UNUSED_PARAMETER!>value1<!>: String, <!UNUSED_PARAMETER!>value2<!>: Any?): String {
    when (value) {
        is <!UNRESOLVED_REFERENCE!>value1<!> -> return ""
        is <!UNRESOLVED_REFERENCE!>value2<!> -> return ""
        is <!UNRESOLVED_REFERENCE!>value1<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>isEmpty<!><!SYNTAX!>(<!><!SYNTAX!><!SYNTAX!><!>)<!><!SYNTAX!><!> <!SYNTAX!><!>-> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with literals as type checking operator value.
fun case_3(value: Any): String {
    when (value) {
        is <!SYNTAX!><!>{} <!SYNTAX!><!>-> return ""
        is <!SYNTAX!><!SYNTAX!><!>fun<!>(<!SYNTAX!><!>) {} <!SYNTAX!><!>-> return ""
        is <!SYNTAX!>90<!> -> return ""
        is <!SYNTAX!>-<!><!SYNTAX!>.032<!><!SYNTAX!><!> <!SYNTAX!><!>-> return ""
        is <!SYNTAX!>"<!><!SYNTAX!>...<!><!SYNTAX!><!SYNTAX!><!>"<!><!SYNTAX!><!> <!SYNTAX!><!>-> return ""
        is <!SYNTAX!>'.'<!> -> return ""
        is <!SYNTAX!>return<!> <!SYNTAX!>1<!><!SYNTAX!><!> <!SYNTAX!><!>-> return ""
        is <!SYNTAX!>throw<!> <!SYNTAX!>Exception<!>(<!SYNTAX!><!>) <!SYNTAX!><!>-> return ""
    }

    return ""
}
