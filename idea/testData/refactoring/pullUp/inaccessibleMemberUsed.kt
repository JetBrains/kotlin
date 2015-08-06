open class A {

}

class <caret>B: A() {
    // INFO: {"checked": "false"}
    private fun foo() = 1

    // INFO: {"checked": "true"}
    private class Z(n: Int)

    // INFO: {"checked": "true"}
    fun bar1() = foo() + 1
    // INFO: {"checked": "true", "toAbstract": "true"}
    fun bar2() = Z(foo() + 1)

    // INFO: {"checked": "true"}
    val x1 = foo() + 1
    // INFO: {"checked": "true", "toAbstract": "true"}
    val x2 = Z(foo() + 1)
}