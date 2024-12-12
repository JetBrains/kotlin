// WITH_STDLIB
// FIR_IDENTICAL
import kotlin.concurrent.atomics.AtomicIntArray
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
fun foo(a: AtomicIntArray) {
    <!DEPRECATION_ERROR!>a[1]<!>
    <!DEPRECATION_ERROR!>a[1]<!> = 2
    a.loadAt(1)
    a.storeAt(1, 1)
}