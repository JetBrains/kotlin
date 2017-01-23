external fun foo(a: String): Int = definedExternally

external class Bar(b: Int, c: Char) {
    fun baz(d: Int) { definedExternally }
}

external object Obj {
    fun test1(e: String) { definedExternally  }
    object Nested {
        fun test2(g: Int) { definedExternally }
    }
}
