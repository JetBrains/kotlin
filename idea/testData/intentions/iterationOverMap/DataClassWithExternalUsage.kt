// WITH_RUNTIME

data class XY(val x: String, val y: String)
fun test(xys: Array<XY>, base: XY) {
    for (<caret>xy in xys) {
        if (xy.x == base.x) {
            println(xy.y)
        }
    }
}