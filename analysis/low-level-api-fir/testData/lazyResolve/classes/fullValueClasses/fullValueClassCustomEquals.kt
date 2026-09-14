// LANGUAGE: +FullValueClasses
package pack

value class Point(val x: Int, val y: Int) {
    override fun equ<caret>als(other: Any?): Boolean = other is Point && x == other.x

    override fun hashCode(): Int = x
}
