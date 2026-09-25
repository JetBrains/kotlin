// LANGUAGE: +FullValueClasses +NameBasedDestructuring -EnableNameBasedDestructuringShortForm

value class Point(val x: Int, val y: String)

fun test(point: Point) {
    val (y, <expr>x</expr>) = point
}
