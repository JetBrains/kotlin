// FIR_IDENTICAL
// !DIAGNOSTICS: -DEPRECATION
@nativeGetter
fun Int.foo(a: String): Int? = definedExternally

external class Bar(b: Int, c: Char) {
    @nativeGetter
    fun baz(d: Int): Any? = definedExternally
}

external object Obj {
    @nativeGetter
    fun test1(e: String): String? = definedExternally

    object Nested {
        @nativeGetter
        fun test2(g: Int): Any? = definedExternally
    }
}
