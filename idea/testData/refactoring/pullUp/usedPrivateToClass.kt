open class A

class <caret>B: A() {
    // INFO: {"checked": "true"}
    private fun foo(): Int = 1

    fun bar() = foo() + 1
}