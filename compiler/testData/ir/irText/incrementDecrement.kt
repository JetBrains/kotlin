var p: Int = 0
val arr = intArrayOf(1, 2, 3)

fun testVar() {
    var x = 0
    val x1 = ++x
    val x2 = --x
}

fun testProp() {
    val p1 = ++p
    val p2 = --p
}

fun testArray() {
    val a1 = ++arr[0]
    val a2 = --arr[0]
}