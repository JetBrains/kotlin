// WITH_RUNTIME

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is Child1 && parent.field1 == 1 -> parent.field1
    !is Child2 -> 10
    !is Child2 -> 12
    else -> parent.field2
}

fun box() = "OK"
