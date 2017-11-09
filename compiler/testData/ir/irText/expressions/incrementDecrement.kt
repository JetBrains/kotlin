var p: Int = 0
val arr = intArrayOf(1, 2, 3)

fun testVarPrefix() {
    var x = 0
    val x1 = ++x
    val x2 = --x
}

fun testVarPostfix() {
    var x = 0
    val x1 = x++
    val x2 = x--
}

fun testPropPrefix() {
    val p1 = ++p
    val p2 = --p
}

fun testPropPostfix() {
    val p1 = p++
    val p2 = --p
}

fun testArrayPrefix() {
    val a1 = ++arr[0]
    val a2 = --arr[0]
}

fun testArrayPostfix() {
    val a1 = arr[0]++
    val a2 = arr[0]--
}