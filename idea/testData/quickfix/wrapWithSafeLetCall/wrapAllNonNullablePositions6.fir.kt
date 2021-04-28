// "Wrap with '?.let { ... }' call" "true"
// ACTION: Add 'a =' to argument
// ACTION: Add non-null asserted (!!) call
// ACTION: Flip '+'
// ACTION: Introduce local variable
// ACTION: Replace overloaded operator with function call
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// WITH_RUNTIME

interface A {
    operator fun plus(a: A): A = this
}

fun test(a1: A?, a2: A) {
    notNull(a1 <caret>+ a2)
}

fun notNull(a: A): A = a
