// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -DEPRECATION
@nativeGetter
fun String.foo(n: Int): Int?
@nativeGetter
fun String.bar(n: Int): Int? = definedExternally


external interface T {
    @nativeGetter
    fun foo(d: Double): String?
    @nativeGetter
    fun bar(d: Double): String?
}