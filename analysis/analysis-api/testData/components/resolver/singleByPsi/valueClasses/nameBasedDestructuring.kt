// LANGUAGE: +FullValueClasses +NameBasedDestructuring -EnableNameBasedDestructuringShortForm

value class Point(val x: Int, val y: String) {
    operator fun component1(): Boolean = true
    operator fun component2(): Boolean = false
}

fun test(point: Point) {
    val (y, <expr>x</expr>) = point
}
