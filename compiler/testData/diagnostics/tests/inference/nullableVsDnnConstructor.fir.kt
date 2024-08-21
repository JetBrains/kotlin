// ISSUE: KT-61227

fun <G> go(t: G) = C<G & Any>(t)

class C<T : Any>(t: T?)
