// !CHECK_TYPE

fun test() {
    val array = arrayOf(arrayOf(1))
    array checkType { <!UNRESOLVED_REFERENCE!>_<!><Array<Array<Int>>>() }
}