fun foo(arr: Array<out Number>): Int {
    val result = (arr as Array<Int>)[0]
    arr : Array<Int>
    return result
}
