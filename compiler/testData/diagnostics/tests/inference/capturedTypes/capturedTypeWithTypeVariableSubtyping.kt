// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

fun takeCovArray(a: Array<out Number>) {}
fun takeInArray(a: Array<in Number>) {}

fun <T> foo(): Array<out T> = TODO()
fun <T> bar(): Array<in T> = TODO()

fun <X> id(x: X): X = x
fun <Z> arrayInId(z: Array<in Z>): Array<in Z> = z
fun <Z> arrayOutId(z: Array<out Z>): Array<out Z> = z

fun test() {
    takeCovArray(foo())
    takeInArray(bar())

    takeCovArray(id(foo()))
    takeInArray(id(bar()))

    takeCovArray(arrayOutId(foo()))
    takeInArray(arrayInId(bar()))
}