// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER

fun <T> id(x: T) = x
fun <T> select(vararg x: T) = x[0]

val x1 = select(id { <!NO_THIS!>this<!> }, fun Int.() = this)
val x2 = select(id { <!NO_THIS!>this<!> + <!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, fun Int.(x: Int) = this)
val x3 = select(id { <!NO_THIS!>this<!>.<!UNRESOLVED_REFERENCE!>length<!> + <!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>inv<!>() }, fun String.(x: Int) = length)
