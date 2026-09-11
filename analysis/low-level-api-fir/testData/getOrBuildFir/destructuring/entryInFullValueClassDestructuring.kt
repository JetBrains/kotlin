// LANGUAGE: +FullValueClasses +NameBasedDestructuring -EnableNameBasedDestructuringShortForm
// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry

value class Point(val x: Int, val y: String)

fun test(point: Point) {
    val (y, <expr>x</expr>) = point
}
