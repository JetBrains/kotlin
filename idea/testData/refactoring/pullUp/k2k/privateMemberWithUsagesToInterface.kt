interface I

class <caret>Foo : I {
    // INFO: {checked: "true"}
    private fun privateFun() = 0

    fun refer() = privateFun()
}