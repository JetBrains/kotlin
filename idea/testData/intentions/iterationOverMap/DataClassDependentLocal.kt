// IS_APPLICABLE: false
// WITH_RUNTIME

data class XY(val x: Int, val y: Int)
fun test(xys: Array<XY>) {
    for (xy in xys) {
        val x = xy.x
        println(x)
        val y = xy.y + x
        println(y)
    }
}