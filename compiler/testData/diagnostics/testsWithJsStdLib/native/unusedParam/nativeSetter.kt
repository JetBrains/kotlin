// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeSetter<!>
fun Int.foo(a: String, v: Int): Int = definedExternally

external class Bar(b: Int, c: Char) {
    @nativeSetter
    fun baz(d: Int, v: Int) { definedExternally }
}

external object Obj {
    @nativeSetter
    fun test1(e: String, v: Any) { definedExternally }

    object Nested {
        @nativeSetter
        fun test2(g: Int, v: Any) { definedExternally }
    }
}
