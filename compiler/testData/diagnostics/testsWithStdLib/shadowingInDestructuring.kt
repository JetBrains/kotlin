data class XY(val x: Int, val y: Int)

fun foo(list: List<XY>) {
    for ((x1, y1) in list) {
        for ((x2, y2) in list) {
            if (x1 == y2 && x2 == y1) return
        }
        list.map { (x3, y3) -> x3 + y3 }
    }
}
