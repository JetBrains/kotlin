open class A {
    open fun bar() = 1
}

class <caret>B: A() {
    override fun bar() = 1

    // INFO: {"checked": "true"}
    fun foo() = super.bar()
}