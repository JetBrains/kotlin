annotation class ann

fun test([ann] <!UNUSED_PARAMETER!>p<!>: Int) {

}

val bar = fun test([ann] <!UNUSED_PARAMETER!>g<!>: Int) {}

val bas = { ([ann] t: Int) -> }