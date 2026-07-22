// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// DONT_TARGET_EXACT_BACKEND: NATIVE
//    ^^^ some native tests don't support API_VERSION directive
//    ^^^ fix or wait till version bump

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
