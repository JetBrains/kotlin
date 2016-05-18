// IS_APPLICABLE: false
// WITH_RUNTIME

data class XY(val x: String, val y: Int)
fun test(xys: Array<XY>) {
    for (<caret>xy in xys) {
        val x = xy.x
        var y = xy.y
        println(x + y)
        y--
        println(y)
    }
}