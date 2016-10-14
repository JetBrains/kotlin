// !API_VERSION: 1.0

@SinceKotlin("1.1")
open class Foo

class Bar @SinceKotlin("1.1") constructor()

@SinceKotlin("1.0")
class Baz @SinceKotlin("1.1") constructor()


fun t1(): <!API_NOT_AVAILABLE!>Foo<!> = <!UNRESOLVED_REFERENCE!>Foo<!>()

// TODO: do not report API_NOT_AVAILABLE twice
fun t2() = object : <!UNRESOLVED_REFERENCE, API_NOT_AVAILABLE, API_NOT_AVAILABLE, DEBUG_INFO_UNRESOLVED_WITH_TARGET!>Foo<!>() {}

fun t3(): Bar? = <!UNRESOLVED_REFERENCE!>Bar<!>()

fun t4(): Baz = <!UNRESOLVED_REFERENCE!>Baz<!>()
