// LANGUAGE: +StrictEquals

data class Point(val x: Int, val y: Int)

fun box(): String {
    if (Point(1, 2) != Point(1, 2)) return "FAIL 1"
    if (Point(1, 2) == Point(1, 3)) return "FAIL 2"
    val p1 = Point(3, 4)
    val any: Any? = Point(3, 4)
    if (p1 == any) {
        if (any.x != 3 || any.y != 4) return "FAIL 3"
    } else {
        return "FAIL 4"
    }
    return "OK"
}
