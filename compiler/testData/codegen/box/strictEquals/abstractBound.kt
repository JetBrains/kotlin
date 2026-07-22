// LANGUAGE: +StrictEquals

abstract class Shape {
    abstract val area: Int
    abstract override fun equals(@EqualityBound(Shape::class) other: Any?): Boolean
}

class Circle(val r: Int) : Shape() {
    override val area: Int get() = r * r
    override fun equals(other: Any?): Boolean = area == other.area
}

class Square(val s: Int) : Shape() {
    override val area: Int get() = s * s
    override fun equals(other: Any?): Boolean = area == other.area
}

fun box(): String {
    val circle: Any? = Circle(3)
    val square: Any? = Square(3)
    val c = Circle(3)
    if (c != circle) return "FAIL 1"
    if (c != square) return "FAIL 2"
    if (c == circle) {
        if (circle.area != 9) return "FAIL 3"
    }
    if (c == Circle(4)) return "FAIL 4"
    return "OK"
}
