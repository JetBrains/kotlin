// LANGUAGE: +FullValueClasses
package pack

value class Point(val x: Int, val y: Int) {
    val su<caret>m: Int get() = x + y
}
