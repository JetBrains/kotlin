// RUN_PIPELINE_TILL: FRONTEND
fun <T, R> use(x: (T) -> R): (T) -> R = x

fun foo() = use(::bar)
fun bar(x: String) = 1

fun loop1() = <!CANNOT_INFER_PARAMETER_TYPE, CANNOT_INFER_PARAMETER_TYPE!>use<!>(::<!INAPPLICABLE_CANDIDATE!>loop2<!>)
fun loop2() = loop1()
