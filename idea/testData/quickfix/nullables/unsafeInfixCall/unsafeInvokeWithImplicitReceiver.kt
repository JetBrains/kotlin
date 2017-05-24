// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Convert to expression body
// ACTION: Replace with safe (this?.) call
// ACTION: Wrap with '?.let { ... }' call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type A?

class A {
    fun foo() {
    }
}

fun A?.bar() {
    <caret>foo()
}