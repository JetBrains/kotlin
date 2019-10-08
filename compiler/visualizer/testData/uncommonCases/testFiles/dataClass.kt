data class Vector(val x: Int, val y: Int) {
    fun plus(other: Vector): Vector = Vector(x + other.x, y + other.y)
}

fun main() {
    val a = Vector(1, 2)
    val b = Vector(-1, 10)

    println("a = $a, b = ${b.toString()}")
    println("a + b = " + (a + b))
    println("a hash - ${a.hashCode()}")

    println("a is equal to b ${a.equals(b)}")
}