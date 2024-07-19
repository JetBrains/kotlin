// ISSUE: KT-61227

fun <G> go(t: G) = C<G & Any>(<!ARGUMENT_TYPE_MISMATCH("G & Any; G")!>t<!>)

class C<T : Any>(t: T?)
