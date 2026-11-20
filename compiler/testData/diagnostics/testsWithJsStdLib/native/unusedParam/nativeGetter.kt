// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
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
