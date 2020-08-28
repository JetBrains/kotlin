// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNUSED_VALUE -UNUSED_PARAMETER -UNUSED_EXPRESSION
// SKIP_TXT


// TESTCASE NUMBER: 1
class Case1() {
    fun foo(x: () -> CharSequence): Unit = TODO() // (3)
    fun foo(x: () -> String, z: String = ""): String = TODO() // (4)

    fun boo() = ""
    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case1.foo; typeCall: function")!>foo(::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo)<!>
        foo(::boo)
    }
}

// TESTCASE NUMBER: 2
class Case2() {
    fun foo(y: ()->Any?, x: ()->Any?): Unit = TODO() // (1.1)
    fun foo(vararg x: ()->Int): String = TODO() // (1.2)

    fun boo() = 1

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo(::boo, ::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo, ::boo)<!>

        <!DEBUG_INFO_CALL("fqName: Case2.foo; typeCall: function")!>foo({1}, {1})<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo({1}, {1})<!>
    }
}

// TESTCASE NUMBER: 3
class Case3() {
    fun foo(x: ()->CharSequence, x1: String = ""): Unit = TODO() // (3)
    fun foo(x: ()->String, z: Any = ""): String = TODO() // (4)

    fun boo() = ""

    val boo = 1

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo(::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo)<!>
        foo(::boo)
        foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.String>")!>::boo<!>)

        <!DEBUG_INFO_CALL("fqName: Case3.foo; typeCall: function")!>foo({ "" })<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo({ "" })<!>
    }
}


// TESTCASE NUMBER: 4
class Case4() {
    fun foo(x: ()->CharSequence, x1: String = ""): Unit = TODO() // (3)
    fun foo(x: ()->String, z: Any = ""): String = TODO() // (4)

    fun boo() = 1

    val boo = ""

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: function")!>foo(::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo)<!>
        foo(::boo)
        foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.String>")!>::boo<!>)

        <!DEBUG_INFO_CALL("fqName: Case4.foo; typeCall: function")!>foo({ "" })<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo({ "" })<!>
    }
}

// TESTCASE NUMBER: 5
class Case5() {
    fun foo(x: ()->CharSequence, x1: String = ""): Unit = TODO() // (3)
    fun foo(x: ()->String, z: Any = ""): String = TODO() // (4)

    fun boo() = "" as CharSequence

    val boo = ""

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case5.foo; typeCall: function")!>foo(::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo)<!>
        foo(::boo)
        foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KProperty0<kotlin.String>")!>::boo<!>)

        <!DEBUG_INFO_CALL("fqName: Case5.foo; typeCall: function")!>foo({ "" })<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo({ "" })<!>
    }
}

// TESTCASE NUMBER: 6
class Case6() {
    fun foo(x: ()->CharSequence, x1: String = ""): Unit = TODO() // (3)
    fun foo(x: ()->String, z: Any = ""): String = TODO() // (4)

    fun boo() = ""

    val boo = "" as CharSequence

    fun case() {
        <!DEBUG_INFO_CALL("fqName: Case6.foo; typeCall: function")!>foo(::boo)<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>foo(::boo)<!>
        foo(::boo)
        foo(<!DEBUG_INFO_EXPRESSION_TYPE("kotlin.reflect.KFunction0<kotlin.String>")!>::boo<!>)
    }
}
