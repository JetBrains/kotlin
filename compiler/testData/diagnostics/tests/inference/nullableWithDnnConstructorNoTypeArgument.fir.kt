// ISSUE: KT-61227

fun <G> go(t: G) = C(t)

class C<T : Any>(t: T?)
