// WITH_RUNTIME

sealed class Parent<T> {
    class Child1<T>(): Parent<T>()
    class Child2<T>(): Parent<T>()
}

fun <T> foo(parent: Parent<T>) = when(parent) {
    is Parent.Child1 -> {}
    is Parent.Child2 -> {}
}

fun box() = "OK"
