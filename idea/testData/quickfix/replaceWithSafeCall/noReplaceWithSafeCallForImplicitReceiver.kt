// "Replace with safe (?.) call" "false"
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace with safe (this?.) call
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type A?

class A {
    fun foo() {
    }
}

fun A?.bar() {
    <caret>foo()
}
