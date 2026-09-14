// LANGUAGE: +FullValueClasses
package pack

value class Point(val x: Int, val y: Int) {
    fun su<caret>m(): Int = x + y
}
