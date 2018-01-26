// WITH_RUNTIME

sealed class Parent

data class Child1(val field1: Int): Parent()

data class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is Child1(val f1) -> parent.field1 + f1
    !is Child2(val _) -> 10
    !is Child2(_) -> 20
    is (val f2) -> parent.field2 + f2
}

fun foo2(parent: Parent) = when(parent) {
    is Child1(val f1) && parent.field1 == 1 -> parent.field1 + f1
    !is Child2(val _) -> 10
    !is Child2(_) -> 20
    is (val f2) && parent.field2 == 2 -> parent.field2 + f2
    else -> 30
}

fun box() = "OK"
