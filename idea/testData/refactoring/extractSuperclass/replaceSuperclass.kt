// NAME: X
interface T {}

open class A

// SIBLING:
class <caret>B : A(), T {
    // INFO: {checked: "true"}
    fun foo() {

    }
}