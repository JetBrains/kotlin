// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
fun case1() {
    val c = <!ILLEGAL_CONST_EXPRESSION!>''<!>
}

// TESTCASE NUMBER: 2

fun case2() {
    val c2: Char = <!ILLEGAL_CONST_EXPRESSION!>''<!><!SYNTAX!>'<!>
    val c3: Char = <!ILLEGAL_CONST_EXPRESSION!>'\'<!>
}

// TESTCASE NUMBER: 3

fun case3() {
    val c1: Char = <!ILLEGAL_CONST_EXPRESSION!>'B a'<!>
    val c2: Char = <!ILLEGAL_CONST_EXPRESSION!>'  '<!>
    val c3: Char = <!ILLEGAL_CONST_EXPRESSION!>'Ba'<!>
}

// TESTCASE NUMBER: 4

fun case4() {
    val cOutOfRaneMin = <!ILLEGAL_CONST_EXPRESSION!>'𐀀'<!> //u+10000

    val cOutOfRangeAroundMax = <!ILLEGAL_CONST_EXPRESSION!>'󠇿󠇿󟿿'<!> //u+Dfffff
}
