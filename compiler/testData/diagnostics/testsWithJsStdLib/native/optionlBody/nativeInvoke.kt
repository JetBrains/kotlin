// !DIAGNOSTICS: -DEPRECATION
@nativeInvoke
fun String.foo(): Int
@nativeInvoke
fun String.bar(): Int = noImpl


external object O {
    @nativeInvoke
    fun foo()
    @nativeInvoke
    fun bar() {}
}