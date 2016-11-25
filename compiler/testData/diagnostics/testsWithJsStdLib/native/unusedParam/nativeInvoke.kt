// !DIAGNOSTICS: -DEPRECATION
@nativeInvoke
fun Int.foo(a: String): Int = noImpl

external class Bar(b: Int, c: Char) {
    @nativeInvoke
    fun baz(d: Int) {}
}

external object Obj {
    @nativeInvoke
    fun test1(e: String) {}

    object Nested {
        @nativeInvoke
        fun test2(g: Int) {}
    }
}
