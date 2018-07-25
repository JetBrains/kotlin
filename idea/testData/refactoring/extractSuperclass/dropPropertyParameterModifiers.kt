// WITH_RUNTIME
// NAME: Middle

open class Parent(open val bad: String) {
    open val good: String = "a"
}

// SIBLING:
class <caret>Child(
    // INFO: {checked: "true"}
    override val bad: String
) : Parent(bad) {
    // INFO: {checked: "true"}
    override val good: String = "b"
}