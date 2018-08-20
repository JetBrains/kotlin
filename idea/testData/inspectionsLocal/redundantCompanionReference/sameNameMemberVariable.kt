// PROBLEM: none

class Test {
    companion object {
        val memberVar = 1
    }

    val memberVar = 2

    fun test() {
        <caret>Companion.memberVar
    }
}