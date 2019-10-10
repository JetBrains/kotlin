// PROBLEM: none

enum class A {
    TEST;

    companion object {
        fun valueOf(name: String) {
        }
    }

    fun test() {
        <caret>Companion.valueOf("TEST")
    }
}