annotation class ann

fun test([ann] <!UNUSED_PARAMETER!>p<!>: Int) {

}

val bar = fun test([ann] <!UNUSED_PARAMETER!>g<!>: Int) {}

val bas = { <!DEPRECATED_LAMBDA_SYNTAX!>([ann] t: Int)<!> -> }