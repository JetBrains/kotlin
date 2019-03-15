// PROBLEM: none

class Test {
    companion object {
        val localVar = 1
    }

    fun test() {
        var localVar = 2

        <caret>Companion.localVar
    }
}