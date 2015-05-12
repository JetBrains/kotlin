// "Cast expression 'a' to 'Foo'" "true"

interface Foo {
    fun plus(x: Any) : Foo
}

open class MyClass {
    public open val a: Any = "42"
}

fun MyClass.foo(): Any {
    if (a is Foo) {
        return a<caret> + a
    }
    return 42
}
