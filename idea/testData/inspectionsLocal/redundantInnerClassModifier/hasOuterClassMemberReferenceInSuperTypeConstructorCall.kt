// PROBLEM: none
abstract class Base(val x: Int)

open class Outer(val x: Int) {
    <caret>inner class Inner {
        fun useX() {
            object : Base(x) {
            }
        }
    }
}