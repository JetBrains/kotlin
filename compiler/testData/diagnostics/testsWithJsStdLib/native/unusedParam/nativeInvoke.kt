// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeInvoke<!>
fun Int.foo(a: String): Int = definedExternally

external class Bar(b: Int, c: Char) {
    @nativeInvoke
    fun baz(d: Int) { definedExternally }
}

external object Obj {
    @nativeInvoke
    fun test1(e: String) { definedExternally }

    object Nested {
        @nativeInvoke
        fun test2(g: Int) { definedExternally }
    }
}
