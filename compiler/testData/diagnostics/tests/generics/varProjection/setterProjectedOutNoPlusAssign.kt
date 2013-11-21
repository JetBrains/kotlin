trait Tr<T> {
    var v: T
}

fun test(t: Tr<out String>) {
    <!SETTER_PROJECTED_OUT!>t.v<!> += null!!
}