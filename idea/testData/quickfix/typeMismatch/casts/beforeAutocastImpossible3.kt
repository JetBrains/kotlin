// "Cast expression 'x' to 'Foo<Number>'" "true"
trait Foo<out T: Number> {
    fun foo(x: T)
}

fun bar(_x: Any) {
    var x = _x
    if (x is Foo<*>) {
        x<caret>.foo(42)
    }
}