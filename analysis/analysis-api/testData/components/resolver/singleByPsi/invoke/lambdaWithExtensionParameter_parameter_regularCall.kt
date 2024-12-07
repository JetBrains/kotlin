package test

class Foo

fun usage(foo: Foo, action: Foo.() -> Unit) {
    foo.acti<caret>on()
}