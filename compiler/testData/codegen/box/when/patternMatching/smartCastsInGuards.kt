// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is Child1 && parent.field1 == 1 -> parent.field1
    is Child2 && parent.field2 == 2 -> parent.field2
    else -> 10
}
fun box(): String {
    assertEquals(foo(Child1(1)), 1)
    assertEquals(foo(Child1(2)), 10)
    assertEquals(foo(Child1(3)), 10)
    assertEquals(foo(Child2(2)), 2)
    assertEquals(foo(Child2(1)), 10)
    assertEquals(foo(Child2(3)), 10)

    return "OK"
}

