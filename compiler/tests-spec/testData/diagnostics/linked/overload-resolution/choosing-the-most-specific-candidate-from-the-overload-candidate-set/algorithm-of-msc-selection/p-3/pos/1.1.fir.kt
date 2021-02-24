// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1() {
    fun foo(x: CharSequence): Unit = TODO() // (3)
    fun foo(x: String, z: String = ""): String = TODO() // (4)

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo("")<!>
    }
}

// TESTCASE NUMBER: 2
class Case2() {
    fun foo(y: Any?, x: Any?): Unit = TODO() // (1.1)
    fun foo(vararg x: Int): String = TODO() // (1.2)

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(1, 1)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(1, 1)<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    fun foo(x: CharSequence, x1: String = ""): Unit = TODO() // (3)
    fun foo(x: String, z: Any = ""): String = TODO() // (4)

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo("")<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo("")<!>
    }
}
