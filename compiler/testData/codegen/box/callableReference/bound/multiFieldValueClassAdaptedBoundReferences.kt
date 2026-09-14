// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int)

fun Point.withDefault(scale: Int = 2): Int = (x + y) * scale
fun Point.withVararg(vararg offsets: Int): Int = x + y + offsets.sum()

fun box(): String {
    val defaultReference: () -> Int = Point(1, 2)::withDefault
    if (defaultReference() != 6) return "FAIL default argument"

    val varargReference: () -> Int = Point(3, 4)::withVararg
    if (varargReference() != 7) return "FAIL vararg"

    var invoked = false
    fun Point.withSideEffect(): Int {
        invoked = true
        return x + y
    }

    val unitReference: () -> Unit = Point(5, 6)::withSideEffect
    unitReference()
    if (!invoked) return "FAIL coercion to Unit"

    return "OK"
}
