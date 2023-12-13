package test

val bigProperty: String = "Hello"

<expr>
class Foo(bigProperty: String) {
    val propertyWithAccessors
        get() = test.bigProperty
        set(value) {
            test.bigProperty
        }

    fun functionWithInitializer() = test.bigProperty
    fun functionWithBody() {
        return test.bigProperty
    }
}
</expr>