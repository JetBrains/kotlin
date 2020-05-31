// !LANGUAGE: +SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

object Test1 {
    fun foo(f: () -> Unit) {}

    object Scope {
        fun foo(f: suspend () -> Unit) {}

        fun test(g: () -> Unit) {
            <!COMPATIBILITY_WARNING, DEBUG_INFO_CALL("fqName: Test1.foo; typeCall: function")!>foo(g)<!>
        }
    }
}

object Test2 {
    fun <T> foo(f: suspend () -> T): T = TODO()
    suspend fun bar(): Int = 0

    object Scope {
        fun bar(): String = ""

        fun test() {
            val result = foo(<!COMPATIBILITY_WARNING!>::bar<!>)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
        }
    }
}