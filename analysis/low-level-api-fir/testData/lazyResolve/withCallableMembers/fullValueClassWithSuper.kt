// LANGUAGE: +FullValueClasses
value class Full<caret>ValueClass(override val first: Int, val second: Int) : Base() {
    override fun baseFunction() {}
}

abstract value class Base : AnotherSuperInterface {
    abstract val first: Int

    fun functionWithDefaultImplementation(i: Int): Int = i

    abstract override fun baseFunction()

    override fun baz() {
        // default implementation
    }
}

interface AnotherSuperInterface {
    fun baz()

    fun baseFunction()
}
