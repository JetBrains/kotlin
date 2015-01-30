// "Cast expression 'x' to 'Foo<*>'" "true"
trait Foo<T: Number> {
    fun foo()
}

fun bar(_x: Any) {
    var x = _x
    if (x is Foo<*>) {
        x<caret>.foo()
    }
}