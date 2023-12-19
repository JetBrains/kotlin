// !DIAGNOSTICS: -UNUSED_PARAMETER

object Test1 {
    fun foo(f: () -> Int) {}

    object Scope {
        fun foo(f: () -> Unit) {}

        fun test(f: () -> Int) {
            <!DEBUG_INFO_CALL("fqName: Test1.foo; typeCall: function")!><!COMPATIBILITY_WARNING!>foo<!>(f)<!>
        }
    }
}

object Test2 {
    fun interface KRunnable {
        fun run()
    }

    fun foo(f: () -> Unit) {}

    object Scope1 {
        fun foo(f: KRunnable) {}

        fun test(f: () -> Int) {
            <!DEBUG_INFO_CALL("fqName: Test2.Scope1.foo; typeCall: function")!>foo(<!UNSUPPORTED_FEATURE!>f<!>)<!>
        }
    }
}

object Test3 {
    fun foo(f: () -> Int) = 1

    fun foo(f: () -> Unit) = "2"

    fun test(f: () -> Int) {
        val result = foo(f)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }
}
