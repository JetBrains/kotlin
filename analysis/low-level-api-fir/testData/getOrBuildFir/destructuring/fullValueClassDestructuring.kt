// LANGUAGE: +FullValueClasses +NameBasedDestructuring -EnableNameBasedDestructuringShortForm
// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtDestructuringDeclaration

value class Point(val x: Int, val y: String)

fun test(point: Point) {
    <expr>val (y, x) = point</expr>
}
