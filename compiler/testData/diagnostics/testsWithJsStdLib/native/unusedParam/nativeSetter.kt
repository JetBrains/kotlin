// !DIAGNOSTICS: -DEPRECATION
@nativeSetter
fun Int.foo(a: String, v: Int): Int = noImpl

external class Bar(b: Int, c: Char) {
    @nativeSetter
    fun baz(d: Int, v: Int) {}
}

external object Obj {
    @nativeSetter
    fun test1(e: String, v: Any) {}

    object Nested {
        @nativeSetter
        fun test2(g: Int, v: Any) {}
    }
}
