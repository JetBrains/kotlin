// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Parent

data class Child1(val field1: Int): Parent()

data class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is Child1(val f1) -> parent.field1 + f1
    is Child2(val f2) -> parent.field2 + f2
}

fun foo2(parent: Parent) = when(parent) {
    is Child1(val f1) && parent.field1 == 1 -> parent.field1 + f1
    is Child2(val f2) && parent.field2 == 2 -> parent.field2 + f2
    else -> 10
}


fun box(): String {
    assertEquals(foo(Child1(1)), 2)
    assertEquals(foo(Child1(2)), 4)
    assertEquals(foo(Child1(3)), 6)
    assertEquals(foo(Child2(2)), 4)
    assertEquals(foo(Child2(1)), 2)
    assertEquals(foo(Child2(3)), 6)

    assertEquals(foo2(Child1(1)), 2)
    assertEquals(foo2(Child1(2)), 10)
    assertEquals(foo2(Child1(3)), 10)
    assertEquals(foo2(Child2(2)), 4)
    assertEquals(foo2(Child2(1)), 10)
    assertEquals(foo2(Child2(3)), 10)
    return "OK"
}
