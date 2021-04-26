// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION -UNREACHABLE_CODE
// SKIP_TXT

// TESTCASE NUMBER: 1
class Case1(val nothing: Nothing)

fun case1() {
    val res = Case1(<!INVISIBLE_REFERENCE!>Nothing<!>())
}


// TESTCASE NUMBER: 2
class Case2 {
    var data: String? = null
}

fun case2(c: Case2) {
    val testValue = c.data ?: throw IllegalArgumentException("data required")
    testValue checkType { <!NONE_APPLICABLE!>check<!><Nothing>() }
}
