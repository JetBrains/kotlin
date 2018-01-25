// WITH_RUNTIME

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is val child: Child1 -> child.field1
    !is val _ : Child2 -> 10
    is val child -> child.field2
}

fun box() = "OK"
