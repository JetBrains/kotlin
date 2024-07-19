// FIR_IDENTICAL
// ISSUE: KT-61227

fun <G> go(t: G) = C<G & Any>(t)

fun <T : Any> C(t: T?) {}
