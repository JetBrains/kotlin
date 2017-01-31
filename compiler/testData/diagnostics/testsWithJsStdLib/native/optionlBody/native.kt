// !DIAGNOSTICS: -UNREACHABLE_CODE
// unreachable code suppressed due to KT-9586

external val baz: Int
external val boo: Int = definedExternally

external fun foo()
external fun bar() { definedExternally }

external interface T {
    val baz: Int

    fun foo()
    fun bar()
}

external class C {
    val baz: Int
    val boo: Int = definedExternally

    fun foo()
    fun bar() { definedExternally }

    companion object {
        val baz: Int
        val boo: Int = definedExternally

        fun foo()
        fun bar(): String = definedExternally
    }
}

external object O {
    val baz: Int
    val boo: Int = definedExternally

    fun foo(s: String): String
    fun bar(s: String): String = definedExternally
}


