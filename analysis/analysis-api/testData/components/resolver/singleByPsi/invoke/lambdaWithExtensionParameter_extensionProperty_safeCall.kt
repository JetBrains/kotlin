package test

class Foo

val Foo.action: () -> Unit get() = {}

fun usage(foo: Foo?) {
    foo?.acti<caret>on()
}