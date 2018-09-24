// PROBLEM: none

class Test {
    override fun equals(other: Any?): Boolean {
        val another = Test()
        if (another.equals<caret>(other)) return true
        return false
    }
}