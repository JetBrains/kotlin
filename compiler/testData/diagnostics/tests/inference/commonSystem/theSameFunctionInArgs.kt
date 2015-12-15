// !CHECK_TYPE

fun test() {
    val array = arrayOf(arrayOf(1))
    array checkType { _<Array<Array<Int>>>() }
}