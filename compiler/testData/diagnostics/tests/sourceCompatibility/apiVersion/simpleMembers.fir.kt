// !API_VERSION: 1.0

@SinceKotlin("1.1")
fun f() {}

@SinceKotlin("1.1")
var p = Unit

@SinceKotlin("1.1.2")
fun z() {}


fun t1() = f()

fun t2() = p

fun t3() { p = Unit }

fun t4() { z() }
