// PROBLEM: none

enum class A {
    TEST;

    companion object {
        fun valueOf(name: String) {
        }
    }
}

fun main() {
    A.<caret>Companion.valueOf("TEST")
}