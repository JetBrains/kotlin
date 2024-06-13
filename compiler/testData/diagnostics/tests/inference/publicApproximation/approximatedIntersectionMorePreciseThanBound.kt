// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface Bound {
    fun foo() {}
}
interface Bound1 : Bound
interface Bound2 : Bound
object First : Bound1, Bound2
object Second : Bound1, Bound2

class Out<out O>(val param: O)

fun <S : Any> anyBound(vararg elements: S): Out<S> = TODO()
fun topLevel() = anyBound(First, Second)

fun test() {
    topLevel().param.foo()
}
