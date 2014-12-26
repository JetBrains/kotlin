nativeInvoke
fun String.foo(): Int
nativeInvoke
fun String.bar(): Int = noImpl


native
object O {
    nativeInvoke
    fun foo()
    nativeInvoke
    fun bar() {}
}