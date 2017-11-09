open class A

class <caret>Foo : A() {
    // INFO: {checked: "true"}
    private fun privateFun() = 0

    fun refer() = privateFun()
}