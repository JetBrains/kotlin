// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun interface KRunnable {
    fun invoke()
}

object Test1 {
    fun foo(x: Any) {}
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(r: KRunnable) {}

        fun test(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test1.Scope.foo; typeCall: function")!>foo(f)<!>
        }
    }
}

object Test2 {
    fun foo(f: () -> String) {}

    object Scope {
        fun foo(r: KRunnable) {}

        fun test(f: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test2.Scope.foo; typeCall: function")!>foo(f)<!>
        }
    }
}

object Test3 {
    fun foo(i: Int, r: KRunnable): Int = 0
    fun foo(n: Number, f: () -> Unit): String = ""

    fun test(f: () -> Unit) {
        val result = foo(1, f)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
    }
}

object Test4 {
    fun foo(i: Int, r: KRunnable): Int = 0
    fun foo(n: Number, f: () -> Unit): String = ""

    fun bar() {}

    fun test() {
        val result = foo(1, ::bar)
        result
    }
}

object Test5 {
    fun foo(x: Any) {}
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(r: KRunnable) {}

        fun test() {
            foo { }
        }
    }
}
