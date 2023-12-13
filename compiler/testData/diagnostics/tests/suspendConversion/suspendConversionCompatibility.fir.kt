// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

object Test1 {
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(f: suspend () -> Unit) {}

        fun test(g: () -> Unit) {
            <!DEBUG_INFO_CALL("fqName: Test1.Scope.foo; typeCall: function")!>foo(g)<!>
        }
    }
}

object Test2 {
    fun <T> foo(f: suspend () -> T): T = TODO()
    suspend fun bar(): Int = 0

    object Scope {
        fun bar(): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}

object Test3 {
    fun <T> foo(f: suspend () -> T): T = TODO()

    suspend fun bar(x: Int = 42): Int = 0

    object Scope {
        fun bar(x: Int = 42): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}
