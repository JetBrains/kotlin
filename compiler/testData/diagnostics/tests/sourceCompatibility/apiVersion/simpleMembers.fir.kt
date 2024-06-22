// API_VERSION: 1.0

@SinceKotlin("1.1")
fun f() {}

@SinceKotlin("1.1")
var p = Unit

@SinceKotlin("1.1.2")
fun z() {}


fun t1() = <!API_NOT_AVAILABLE!>f<!>()

fun t2() = <!API_NOT_AVAILABLE!>p<!>

fun t3() { <!API_NOT_AVAILABLE!>p<!> = Unit }

fun t4() { <!API_NOT_AVAILABLE!>z<!>() }
