// "Cast expression 'x' to 'Foo<*>'" "true"

interface Foo<T: Number> {
    fun bar()
}

open class MyClass {
    public open val x: Any = "42"
}

fun MyClass.bar() {
    if (x is Foo<*>) {
        x<caret>.bar()
    }
}