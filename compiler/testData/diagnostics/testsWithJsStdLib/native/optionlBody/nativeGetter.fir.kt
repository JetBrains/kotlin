// !DIAGNOSTICS: -DEPRECATION
<!NON_MEMBER_FUNCTION_NO_BODY!>@nativeGetter
fun String.foo(n: Int): Int?<!>
@nativeGetter
fun String.bar(n: Int): Int? = definedExternally


external interface T {
    @nativeGetter
    fun foo(d: Double): String?
    @nativeGetter
    fun bar(d: Double): String?
}
