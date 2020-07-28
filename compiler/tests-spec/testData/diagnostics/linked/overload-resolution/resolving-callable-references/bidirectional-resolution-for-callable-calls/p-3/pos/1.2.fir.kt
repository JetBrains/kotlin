// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1 {
    fun boo(y: () -> Int, x: () -> Number): Unit = TODO()
    fun boo(vararg x: () -> Int): String = TODO()

    val x = 1.0
    fun x() = 1

    fun case() {
        this.boo(::x, ::x)
        this.<!DEBUG_INFO_CALL("fqName: Case1.boo; typeCall: function")!>boo(::x, ::x)<!>
        this.boo(::x, ::x)
    }
}

// TESTCASE NUMBER: 2
class Case2 {
    fun boo(y: () -> Int, x: () -> Number): Unit = TODO()
    fun boo(vararg x: () -> Int): String = TODO()

    val x = 1
    fun x() = 1.0

    fun case() {
        this.boo(::x, ::x)
        this.<!DEBUG_INFO_CALL("fqName: Case2.boo; typeCall: function")!>boo(::x, ::x)<!>
        this.boo(::x, ::x)
    }
}
