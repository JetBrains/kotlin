// !CHECK_TYPE

fun test() {
    val array = <!REIFIED_TYPE_UNSAFE_SUBSTITUTION!>arrayOf<!>(arrayOf(1))
    array checkType { _<Array<Array<Int>>>() }
}