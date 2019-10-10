// PROBLEM: none

enum class A {
    TEST;

    companion object {
        fun values() {
        }
    }

    fun test() {
        <caret>Companion.values()
    }
}