// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_EXPRESSION

object Test1 {
    fun <T> foo(f: () -> T): T = f()
    fun bar(): Int = 0

    object Scope {
        fun bar(x: Int = 0): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}

object Test2 {
    fun foo(f: () -> Unit) {}
    fun bar() {}

    object Scope {
        fun bar(): String = ""

        fun test() {
            foo(::bar)
        }
    }
}

object Test3 {
    fun <T> foo(f: (Int, Int) -> T): T = TODO()
    fun bar(x: Int, y: Int): Int = 0

    object Scope {
        fun bar(vararg ints: Int): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}

object Test4 {
    fun <T> foo(f: (Array<String>) -> T): T = TODO()
    fun bar(g: Array<String>): Int = 0

    object Scope {
        // Works before 1.4
        fun bar(vararg g: String): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.String")!>result<!>
        }
    }
}

object Test5 {
    fun <T> foo(f: () -> T): T = f()

    object Scope {
        fun bar(): Int = 0

        fun bar(x: Int = 0): String = ""

        fun test() {
            val result = foo(::bar)
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Int")!>result<!>
        }
    }
}
