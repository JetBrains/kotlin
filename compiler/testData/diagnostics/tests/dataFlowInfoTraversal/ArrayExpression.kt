fun foo(arr: Array<out Number>): Int {
    val result = (arr as Array<Int>)[0]
    <!DEBUG_INFO_SMARTCAST!>arr<!> : Array<Int>
    return result
}
