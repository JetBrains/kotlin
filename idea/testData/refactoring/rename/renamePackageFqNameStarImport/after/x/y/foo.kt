package x.y

class Foo

fun Foo.foo(block: Bar.() -> Unit) {
    Bar().block()
}