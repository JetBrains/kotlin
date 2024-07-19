// ISSUE: KT-61227

fun <G> go(t: G) = C<G & Any>(<!TYPE_MISMATCH, TYPE_MISMATCH!>t<!>)

class C<T : Any>(t: T?)
