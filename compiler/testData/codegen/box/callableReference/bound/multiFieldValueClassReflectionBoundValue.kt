// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int)

fun Point.render(delta: Int): String = "${x + delta}:${y + delta}"

fun box(): String {
    val first = Point(1, 2)::render
    val equalReceiver = Point(1, 2)::render
    val differentReceiver = Point(1, 3)::render

    if (first != equalReceiver) return "FAIL1"
    if (first.hashCode() != equalReceiver.hashCode()) return "FAIL2"
    if (first == differentReceiver) return "FAIL3"

    val result = first(10)
    return if (result == "11:12") "OK" else "FAIL"
}
