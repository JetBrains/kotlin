// LANGUAGE: +FullValueClasses
// ISSUE: KT-86207

fun interface Renderer {
    fun render(): String
}

value class Point(val x: Int, val y: Int)

fun Point.render(): String = "P:$x:$y"

fun box(): String {
    val point = Point(1, 2)

    val function: () -> String = point::render
    val renderer = Renderer(point::render)

    val first = function()
    if (first != "P:1:2") return "FAIL function"

    val second = renderer.render()
    if (second != "P:1:2") return "FAIL renderer"

    return "OK"
}
