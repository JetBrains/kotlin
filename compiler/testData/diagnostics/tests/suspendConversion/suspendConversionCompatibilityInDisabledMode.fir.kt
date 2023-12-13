// !LANGUAGE: -SuspendConversion
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

object Test1 {
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

object Test2 {
    fun <T> foo(f: suspend () -> T): T = TODO()

    suspend fun bar(): Int = 0

    object Scope1 {
        suspend fun bar(x: Int = 42): Double = 0.0

        object Scope2 {
            fun bar(x: Int = 42): String = ""

            fun test() {
                val result = foo(::bar)
                <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
            }
        }
    }
}
