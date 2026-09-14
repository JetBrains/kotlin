// LANGUAGE: +FullValueClasses
package pack

value class Point(val x: Int, val y: Int = 0) {
    <caret>constructor(value: Int) : this(value, value)
}
