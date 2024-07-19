// ISSUE: KT-61227

fun <G> go(t: G) = C(<!TYPE_MISMATCH!>t<!>)

class C<T : Any>(t: T?)
