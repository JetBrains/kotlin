// "Cast expression 'a' to 'Foo'" "true"

trait Foo {
    fun not() : Foo
}

open class MyClass {
    public open val a: Any = "42"
}

fun MyClass.foo(): Any {
    if (a is Foo) {
        return !(a as Foo)
    }
    return 42
}