// IGNORE_BACKEND: JVM
// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses

value class Point(val x: Int, val y: Int) : Any()

abstract value class Shape : Any() {
    abstract val size: Int
}

value class Square(override val size: Int) : Shape()

fun box(): String {
    if (!Point::class.java.isValue) return "Point is not a value class"
    if (!Shape::class.java.isValue) return "Shape is not a value class"
    val shape: Shape = Square(3)
    if (!shape.javaClass.isValue) return "Square is not a value class"
    if (Point(1, 2) != Point(1, 2) || shape.size != 3) return "Fail"
    return "OK"
}
