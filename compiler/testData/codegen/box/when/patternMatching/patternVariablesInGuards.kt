// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is val child: Child1 && child.field1 == 1 -> child.field1
    is val child: Child2 && child.field2 == 2  -> child.field2
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
