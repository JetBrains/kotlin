open class A {
    val <caret>foo: String
        get() {
            return "A"
        }
}

class B: A {
    fun bar() {
        println(foo)
    }

    override val foo: String
        get() {
            return "B"
        }
}