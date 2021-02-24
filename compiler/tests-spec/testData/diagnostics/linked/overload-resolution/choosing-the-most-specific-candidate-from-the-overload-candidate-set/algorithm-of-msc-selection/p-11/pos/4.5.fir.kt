// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 5
class Case5 {
    fun <T> List<T>.foo(x: T, y: Int = 1, z: Int = 1)  : Unit =TODO()

    fun <T> List<T>.foo(x: T, y: Int = 1) : String =TODO()

    fun case(list: List<Int>) {
        list.<!DEBUG_INFO_CALL("fqName: Case5.foo; typeCall: extension function")!>foo(1)<!>
        list.foo(1)
    }
}
