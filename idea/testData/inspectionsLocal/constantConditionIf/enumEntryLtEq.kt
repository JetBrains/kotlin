// PROBLEM: Condition is always 'true'
enum class Enum {
    A, B, C
}

fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>Enum.B <= Enum.C) 1 else 2)
}