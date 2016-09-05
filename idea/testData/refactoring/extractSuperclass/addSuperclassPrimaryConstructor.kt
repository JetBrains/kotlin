// NAME: X
interface T {}

// SIBLING:
class <caret>A(n: Int) : T {
    // INFO: {checked: "true"}
    fun foo() {

    }
}