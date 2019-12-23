// SKIP_TXT
// !LANGUAGE: +NewInference

interface Self<E : Self<E>> {
    val x: E
}
fun bar(): Self<*> = TODO()
fun foo(): Self<*>? = TODO()

interface OutSelf<out E : OutSelf<E>> {
    val x: E
}
fun outBar(): OutSelf<*> = TODO()
fun outFoo(): OutSelf<*>? = TODO()

fun main() {
    bar().x.x.x.x.x.x.x.x.x.x.x.x.x // OK
    foo()!!.x.x.x.x.x.x // OK
    foo()!!.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x // unresolved x in NI

    outBar().x.x.x.x.x.x.x.x.x.x.x.x.x // OK
    outFoo()!!.x.x.x.x.x.x // OK
    outFoo()!!.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x // unresolved x in NI
}
