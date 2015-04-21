// !CHECK_TYPE

fun foo(arr: Array<out Number>): Int {
    val result = (arr as Array<Int>)[0]
    checkSubtype<Array<Int>>(<!DEBUG_INFO_SMARTCAST!>arr<!>)
    return result
}
