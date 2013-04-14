// "Cast expression 'x' to 'Foo'" "true"
trait Foo {
    fun foo()
}

fun foo(_x: Any) {
    var x = _x
    if (x is Foo) {
        (x as Foo).foo()
    }
}