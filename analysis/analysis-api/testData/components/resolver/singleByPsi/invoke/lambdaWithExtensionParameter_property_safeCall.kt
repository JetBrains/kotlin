package test

class Foo {
    val action: () -> Unit = {}
}

fun usage(foo: Foo?) {
    foo?.acti<caret>on()
}