// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

var hits = 0

value class Point(val x: Int, val y: Int)

fun Point.render(delta: Int): String = "${x + delta}:${y + delta}"

fun provider(): Point {
    hits++
    return Point(1, 2)
}

fun box(): String {
    val reference: (Int) -> String = provider()::render
    if (hits != 1) return "FAIL hits after creation"

    val first = reference(10)
    if (first != "11:12") return "FAIL first call"

    val second = reference(20)
    if (second != "21:22") return "FAIL second call"

    if (hits != 1) return "FAIL hits after calls"

    return "OK"
}
