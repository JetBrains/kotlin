// "Cast expression 'a' to 'Foo'" "true"
trait Foo {
    fun not() : Foo
}

fun foo(_a: Any): Any {
    var a = _a
    if (a is Foo) {
        return !(a as Foo)
    }
    return 42
}