/*
 KOTLIN SPEC TEST (POSITIVE)

 SECTION 16.30: When expression
 PARAGRAPH: 3
 SENTENCE 2: Each entry consists of a boolean condition (or a special else condition), each of which is checked and evaluated in order of appearance.
 NUMBER: 3
 DESCRIPTION: 'When' without bound value and different variants of the boolean conditions (various objects).
 */

open class A {}
open class B: A() {}
open class C: B() {}
object D {}

// CASE DESCRIPTION: 'When' without 'else' branch.
fun case_1(value1: Any, value2: Any, value3: A, value4: B, value5: C, value6: D, value7: Any, value8: Any, value9: Any): String {
    when {
        value1 === value2 -> return ""
        value3 == value2 -> return ""
        value1 is D -> return ""
        value1 is C -> return ""
        <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value1 === (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value2 === (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value7 === (value8 === value9)<!>)<!>)<!> -> return ""
        value1 == (value2 == (value7 == (value8 == value9))) -> return ""
        value1 == (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value2 === (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value7 === (value8 == value9)<!>)<!>) -> return ""
        <!USELESS_IS_CHECK!>value4 is A<!> && <!USELESS_IS_CHECK!>value5 is C<!> || <!USELESS_IS_CHECK!>value6 is Any<!> -> return ""
    }

    return ""
}

// CASE DESCRIPTION: 'When' with 'else' branch.
fun case_2(value1: Any, value2: Any, value3: A, value4: B, value5: C, value6: D, value7: Any, value8: Any, value9: Any): String {
    return when {
        value1 === value2 -> return ""
        value3 == value2 -> return ""
        value1 is D -> return ""
        value1 is C -> return ""
        <!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value1 === (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value2 === (value7 === value8)<!>)<!> -> return ""
        value1 == (value2 == (value7 == value8)) -> return ""
        value1 == (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value2 === (<!IMPLICIT_BOXING_IN_IDENTITY_EQUALS!>value7 === (value8 == value9)<!>)<!>) -> return ""
        <!USELESS_IS_CHECK!>value4 is A<!> && <!USELESS_IS_CHECK!>value5 is C<!> || <!USELESS_IS_CHECK!>value6 is Any<!> -> return ""
        else -> ""
    }
}