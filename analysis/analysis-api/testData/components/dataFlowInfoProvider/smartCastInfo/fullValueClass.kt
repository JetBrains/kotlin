// LANGUAGE: +FullValueClasses

value class Point(val x: Int, val y: Int)

fun test(value: Any) {
    if (value is Point) {
        <expr>value</expr>.x
    }
}
