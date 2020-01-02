// !API_VERSION: 1.0

@SinceKotlin("1.1")
open class Foo

class Bar @SinceKotlin("1.1") constructor()

@SinceKotlin("1.0")
class Baz @SinceKotlin("1.1") constructor()

@SinceKotlin("1.1")
class Quux @SinceKotlin("1.0") constructor()

fun t1(): Foo = Foo()

// TODO: do not report API_NOT_AVAILABLE twice
fun t2() = object : Foo() {}

fun t3(): Bar? = Bar()

fun t4(): Baz = Baz()

fun t5(): Quux = Quux()
