// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

object Test0 {
    fun foo(f: Runnable): Int = 0
    fun foo(x: () -> String): String = ""

    fun test(f: () -> Unit) {
        val result = foo(f)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }
}

object Test1 {
    fun foo(x: Any) {}
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(r: Runnable) {}

        fun test(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test1.foo; typeCall: function")!><!COMPATIBILITY_WARNING!>foo<!>(f)<!>
        }
    }
}

object Test2 {
    fun foo(f: () -> String) {}

    object Scope {
        fun foo(r: Runnable) {}

        fun test(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test2.Scope.foo; typeCall: function")!>foo(f)<!>
        }
    }
}

object Test3 {
    fun foo(i: Int, r: Runnable): Int = 0
    fun foo(n: Number, f: () -> Unit): String = ""

    fun test(f: () -> Unit) {
        val result = foo(1, f)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
    }
}

object Test4 {
    fun foo(i: Int, r: Runnable): Int = 0
    fun foo(n: Number, f: () -> Unit): String = ""

    fun bar() {}

    fun test() {
        val result = foo(1, ::bar)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
    }
}

object Test5 {
    fun foo(x: Any) {}
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(r: Runnable) {}

        fun test() {
            <!DEBUG_INFO_CALL("fqName: Test5.foo; typeCall: function")!><!COMPATIBILITY_WARNING!>foo<!> { }<!>
        }
    }
}

object Test6 {
    fun foo(x: Any) = 1
    fun foo(f: () -> Unit) = 2.0
    fun foo(r: Runnable) = "3"

    fun test() {
        val result = foo { }
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Double")!>result<!>
    }
}