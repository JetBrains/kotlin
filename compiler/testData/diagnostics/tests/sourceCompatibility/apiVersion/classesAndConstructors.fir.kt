// !API_VERSION: 1.0

@SinceKotlin("1.1")
open class Foo

class Bar @SinceKotlin("1.1") constructor()

@SinceKotlin("1.0")
class Baz @SinceKotlin("1.1") constructor()

@SinceKotlin("1.1")
class Quux @SinceKotlin("1.0") constructor()

fun t1(): <!API_NOT_AVAILABLE!>Foo<!> = <!API_NOT_AVAILABLE!>Foo<!>()

// TODO: do not report API_NOT_AVAILABLE twice
fun t2() = object : <!API_NOT_AVAILABLE!>Foo<!>() {}

fun t3(): Bar? = <!UNRESOLVED_REFERENCE!>Bar<!>()

fun t4(): Baz = <!UNRESOLVED_REFERENCE!>Baz<!>()

fun t5(): <!API_NOT_AVAILABLE!>Quux<!> = <!API_NOT_AVAILABLE!>Quux<!>()
