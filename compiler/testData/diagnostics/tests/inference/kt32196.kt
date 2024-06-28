// DIAGNOSTICS: -UNUSED_PARAMETER

class Inv<T>

fun <R : Any> Inv<Int>.mapNotNull(transform: (Int) -> R?): Inv<R> = null!!

fun test(inv: Inv<Int>) {
    <!DEBUG_INFO_EXPRESSION_TYPE("Inv<kotlin.Nothing>")!>inv.mapNotNull { null }<!>
}