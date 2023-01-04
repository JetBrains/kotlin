// FIR_IDENTICAL
// !DIAGNOSTICS: -DEPRECATION
@nativeInvoke
fun String.foo(): Int
@nativeInvoke
fun String.bar(): Int = definedExternally


external object O {
    @nativeInvoke
    fun foo()
    @nativeInvoke
    fun bar() { definedExternally }
}