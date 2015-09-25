annotation class ann

fun test(@ann <!UNUSED_PARAMETER!>p<!>: Int) {

}

val bar = fun(@ann <!UNUSED_PARAMETER!>g<!>: Int) {}
