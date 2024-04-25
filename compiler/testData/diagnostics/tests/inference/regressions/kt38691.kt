// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE

class Inv<T>
fun <T> materializeInv() = Inv<T>()
fun <X> foo(x: Inv<X>, y: X) = materializeInv<X>()
fun <X> foo(x: Inv<X>, y: () -> X) = materializeInv<X>()

fun <R> main(fn: () -> R) {
    fun bar(): R = null <!UNCHECKED_CAST!>as R<!>
    val x1 = foo<R>(materializeInv()) { fn() } // OVERLOAD_RESOLUTION_AMBIGUITY only in NI
    val x2 = foo<R>(materializeInv(), fn) // OK
    val x3 = foo<R>(materializeInv(), ::bar) // OK
}
