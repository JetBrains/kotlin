// PROBLEM: Condition is always 'true'
import test.Enum.*

fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>A == A) 1 else 2)
}