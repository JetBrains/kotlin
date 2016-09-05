// NAME: X
interface T {}

// SIBLING:
class <caret>A : T {
    // INFO: {checked: "true"}
    fun foo() {

    }
}