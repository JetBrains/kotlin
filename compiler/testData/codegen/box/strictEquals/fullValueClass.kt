// LANGUAGE: +StrictEquals
// LANGUAGE: +FullValueClasses

var callCount = 0

value class Vec(val x: Int, val y: Int) {
    override fun equals(@EqualityBound(Vec::class) other: Any?): Boolean {
        callCount++
        return x == other.x && y == other.y
    }
}

fun box(): String {
    val v: Any = Vec(1, 2)
    callCount = 0
    if (v != v) return "FAIL 1"
    if (callCount == 0) return "FAIL 2: no === guard expected for value class"
    return "OK"
}
