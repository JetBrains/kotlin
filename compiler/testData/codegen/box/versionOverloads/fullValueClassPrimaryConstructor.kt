// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-89523
@file:OptIn(ExperimentalVersionOverloading::class)

value class Point(
    val x: Int,
    @IntroducedAt("2.0") val y: Int = 0,
)

fun box(): String {
    val old = Point(1)
    if (old.x != 1) return "FAIL old x: ${old.x}"
    if (old.y != 0) return "FAIL old y: ${old.y}"

    val new = Point(1, 2)
    if (new.x != 1) return "FAIL new x: ${new.x}"
    if (new.y != 2) return "FAIL new y: ${new.y}"

    return "OK"
}
