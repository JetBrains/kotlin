interface Point {
    val x: Int
    val y: Int
}

class PointImpl(override val x: Int, override val y: Int) : Point

fun foo() {
    val p: Point = PointImpl(1, 2)
    val a: <!REDUNDANT_EXPLICIT_TYPE!>Boolean<!> = true
    val i: Int = 2 * 2
    val l: <!REDUNDANT_EXPLICIT_TYPE!>Long<!> = 1234567890123L
    val s: String? = null
    val sh: Short = 42
}

class My {
    val x: Int = 1
}

object Obj {}

fun bar() {
    val o: <!REDUNDANT_EXPLICIT_TYPE!>Obj<!> = Obj
}

fun doo() {
    val i: <!REDUNDANT_EXPLICIT_TYPE!>Int<!> = 42
    val pi: <!REDUNDANT_EXPLICIT_TYPE!>Float<!> = 3.14f
    val pi2: <!REDUNDANT_EXPLICIT_TYPE!>Double<!> = 3.14
    val ch: <!REDUNDANT_EXPLICIT_TYPE!>Char<!> = 'z'
}

fun soo() {
    val s: <!REDUNDANT_EXPLICIT_TYPE!>String<!> = "Hello ${10+1}"
}

val ZERO: Int = 0

fun main() {
    val id: Id = 11
}

typealias Id = Int