// SKIP_TXT
// !LANGUAGE: +NewInference

interface Self<E : Self<E>> {
    val x: E
}
fun bar(): Self<*> = TODO()

interface OutSelf<out E : OutSelf<E>> {
    val x: E
}
fun outBar(): OutSelf<*> = TODO()

fun <X> id(x: X): X = x

fun main() {
    bar().x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
    id(bar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x

    outBar().x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
    id(outBar()).x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.x
}
