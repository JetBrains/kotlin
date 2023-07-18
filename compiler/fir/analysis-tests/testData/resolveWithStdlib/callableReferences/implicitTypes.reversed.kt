fun <T, R> use(x: (T) -> R): (T) -> R = x

fun foo() = use(::bar)
fun bar(x: String) = 1

fun loop1() = use(::<!UNRESOLVED_REFERENCE!>loop2<!>)
fun loop2() = loop1()
