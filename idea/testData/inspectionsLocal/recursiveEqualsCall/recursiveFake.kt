// PROBLEM: none

class Test {
    override fun equals(other: Any?): Boolean {
        val another = Test()
        if (<caret>another == other) return true
        return false
    }
}