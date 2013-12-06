fun foo(arr: Array<out Number>): Int {
    val result = (arr as Array<Int>)[0]
    <!DEBUG_INFO_AUTOCAST!>arr<!> : Array<Int>
    return result
}
