// PROBLEM: none

class Test {
    override fun equals(other: Any?): Boolean {
        val s = Test()
        if (<caret>this == s) return true
        return false
    }
}