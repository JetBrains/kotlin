// WITH_RUNTIME

data class XY(val x: String, val y: String)
fun test(xys: Array<XY>) {
    for (<caret>xy in xys) {
        val xx = xy.x
        println(xx + xy.y)
    }
}