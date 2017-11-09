class A {
    val field = foo()

    fun <caret>foo(): Int {
        return 1
    }
}