// NAME: X
class A {
    open class B

    // SIBLING:
    class <caret>C : B() {
        // INFO: {checked: "true"}
        private fun foo() {

        }

        fun test() {
            foo()
        }
    }
}