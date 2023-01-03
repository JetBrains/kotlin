// !DIAGNOSTICS: -DEPRECATION
<!NON_MEMBER_FUNCTION_NO_BODY!>@nativeInvoke
fun String.foo(): Int<!>
@nativeInvoke
fun String.bar(): Int = definedExternally


external object O {
    @nativeInvoke
    fun foo()
    @nativeInvoke
    fun bar() { definedExternally }
}
