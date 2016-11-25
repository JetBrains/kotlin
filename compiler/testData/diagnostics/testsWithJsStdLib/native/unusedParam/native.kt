external fun foo(a: String): Int = noImpl

external fun Int.foo(a: String): Int = noImpl

external class Bar(b: Int, c: Char) {
    fun baz(d: Int) {}
}

external object Obj {
    fun test1(e: String) {}
    object Nested {
        fun test2(g: Int) {}
    }
}
