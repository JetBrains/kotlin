// "Cast expression 'a' to 'Foo'" "true"
trait Foo {
    fun plus(x: Any) : Foo
}

fun foo(_a: Any): Any {
    var a = _a
    if (a is Foo) {
        return a<caret> + a
    }
    return 42
}
