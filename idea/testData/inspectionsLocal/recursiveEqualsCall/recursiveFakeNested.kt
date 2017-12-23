// PROBLEM: none
open class Outer {
    inner class Nested : Outer() {
        override fun equals(other: Any?): Boolean {
            if (this@Outer <caret>== other) return true
            return false
        }
    }
}