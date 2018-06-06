// PROBLEM: none

class Test {
    companion object {
        fun localFun() = 1
    }

    fun test() {
        fun localFun() = 2

        <caret>Companion.localFun()
    }
}