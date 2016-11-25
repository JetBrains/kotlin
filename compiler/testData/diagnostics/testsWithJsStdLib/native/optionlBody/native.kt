// !DIAGNOSTICS: -UNREACHABLE_CODE
// unreachable code suppressed due to KT-9586

external val baz: Int
external val boo: Int = noImpl

external val Int.baz: Int

external fun foo()
external fun bar() {}

external fun String.foo(): Int
external fun String.bar(): Int = noImpl

external interface T {
    val baz: Int

    fun foo()
    fun bar() {}

    companion object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

external class C {
    val baz: Int
    val boo: Int = noImpl

    fun foo()
    fun bar() {}

    companion object {
        val baz: Int
        val boo: Int = noImpl

        fun foo()
        fun bar(): String = noImpl
    }
}

external object O {
    val baz: Int
    val boo: Int = noImpl

    fun foo(s: String): String
    fun bar(s: String): String = noImpl
}


