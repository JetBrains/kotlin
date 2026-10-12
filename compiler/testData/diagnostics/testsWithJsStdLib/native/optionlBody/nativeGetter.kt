// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -DEPRECATION
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
fun String.foo(n: Int): Int?
<!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
fun String.bar(n: Int): Int? = definedExternally


external interface T {
    @nativeGetter
    fun foo(d: Double): String?
    @nativeGetter
    fun bar(d: Double): String?
}
