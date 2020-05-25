// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1

fun case1() {
    //less then four hex digits
    val c0 = <!ILLEGAL_CONST_EXPRESSION!>'\u'<!>
    val c1 = <!ILLEGAL_CONST_EXPRESSION!>'\uf'<!>
    val c2 = <!ILLEGAL_CONST_EXPRESSION!>'\u1f'<!>
    val c3 = <!ILLEGAL_CONST_EXPRESSION!>'\u1wf'<!>

    //more then four hex digits
    val c4 = <!ILLEGAL_CONST_EXPRESSION!>'\u1wF2f'<!>
}

// TESTCASE NUMBER: 2

fun case2() {
    //not hex
    val c1 = <!ILLEGAL_CONST_EXPRESSION!>'\u000g'<!>
    val c2 = <!ILLEGAL_CONST_EXPRESSION!>'\u000G'<!>
}
