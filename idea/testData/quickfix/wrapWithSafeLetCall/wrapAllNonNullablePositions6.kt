// "Wrap with '?.let { ... }' call" "false"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ACTION: Add 'a =' to argument
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '+'
// ACTION: Introduce local variable
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// ERROR: Operator call corresponds to a dot-qualified call 'a1.plus(a2)' which is not allowed on a nullable receiver 'a1'.
// WITH_RUNTIME

interface A {
    operator fun plus(a: A): A = this
}

fun test(a1: A?, a2: A) {
    notNull(a1 <caret>+ a2)
}

fun notNull(a: A): A = a
