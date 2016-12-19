external fun foo(a: String): Int = noImpl

external class Bar(b: Int, c: Char) {
    fun baz(d: Int) { noImpl }
}

external object Obj {
    fun test1(e: String) { noImpl  }
    object Nested {
        fun test2(g: Int) { noImpl }
    }
}
