// ISSUE: KT-61227

fun <G> go(t: G) = <!CANNOT_INFER_PARAMETER_TYPE!>C<!>(<!ARGUMENT_TYPE_MISMATCH("kotlin.Any; G")!>t<!>)

class C<T : Any>(t: T?)
