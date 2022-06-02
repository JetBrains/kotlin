// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT

// TESTCASE NUMBER: 1
class Case1() {
    operator fun rangeTo(o: Case1): Nothing?{
        TODO()
    }
}
fun case1() {
    val x = Case1() .. Case1()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Nothing?")!>x<!>
    x checkType { check<Nothing?>() }
}


// TESTCASE NUMBER: 2
class Case2() {
    operator fun rangeTo(o: Case2): Any?{
        TODO()
    }
}
fun case2() {
    val x = Case2() .. Case2()
    <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x<!>
    x checkType { check<Any?>() }
}
