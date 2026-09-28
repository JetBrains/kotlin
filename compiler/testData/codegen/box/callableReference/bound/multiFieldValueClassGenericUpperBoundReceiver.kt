// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

value class Point(val x: Int, val y: Int)

fun Point.render(): String = "P:$x:$y"

fun <T : Point> bind(point: T): () -> String = point::render

fun box(): String {
    val genericReceiver: () -> String = bind(Point(3, 4))

    val result = genericReceiver()
    if (result != "P:3:4") return "FAIL generic receiver"

    return "OK"
}
