// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int)

fun Point.render(): String = "P:$x:$y"
fun String.render(): String = this

fun box(): String {
    val pointReceiver: () -> String = Point(1, 2)::render
    val stringReceiver: () -> String = "OK"::render

    val first = pointReceiver()
    if (first != "P:1:2") return "FAIL point receiver"

    val second = stringReceiver()
    if (second != "OK") return "FAIL string receiver"

    return "OK"
}
