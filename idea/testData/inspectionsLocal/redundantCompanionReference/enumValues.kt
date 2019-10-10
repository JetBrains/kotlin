// PROBLEM: none

enum class A {
    TEST;

    companion object {
        fun values() {
        }
    }
}

fun main() {
    A.<caret>Companion.values()
}