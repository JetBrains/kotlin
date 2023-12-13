// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

fun <T> foo(f: () -> T): T = f()

fun bar(): Unit {}

object Scope {
    fun bar(s: String = ""): Double = 0.0

    fun <T> foo(f: () -> T): T = f()

    object NestedScope {
        fun bar(a: Int = 0): String = ""

        fun test() {
            // Despite the fact ::bar is resolved with compatibility warning, it's important not to propagate it to the outer call
            val result = <!DEBUG_INFO_CALL("fqName: Scope.foo; typeCall: function")!>foo(::bar)<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}
