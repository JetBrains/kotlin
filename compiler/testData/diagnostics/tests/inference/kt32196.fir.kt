// DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T>

fun <R : Any> Inv<Int>.mapNotNull(transform: (Int) -> R?): Inv<R> = null!!

fun test(inv: Inv<Int>) {
    inv.mapNotNull { null }
}