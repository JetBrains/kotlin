// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    fun boo(y: Int, x: Number): Unit = TODO()
    fun boo(vararg x: Int): String = TODO()
    fun case() {
        this.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: function")!>boo(1, 1)<!>
        this.boo(1, 1)
    }
}
