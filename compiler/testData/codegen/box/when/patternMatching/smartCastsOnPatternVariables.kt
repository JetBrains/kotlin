// WITH_RUNTIME

import kotlin.test.assertEquals

sealed class Parent

class Child1(val field1: Int): Parent()

class Child2(val field2: Int): Parent()

fun foo(parent: Parent) = when(parent) {
    is val child: Child1 -> parent.field1
    !is val _ : Child2 -> 10
    is val child -> parent.field2
}

fun foo2(parent: Parent) = when(parent) {
    is val child: Child1 -> child.field1
    !is val _ : Child2 -> 10
    is val child -> child.field2
}

fun foo3(parent: Parent) = when(parent) {
    is val child: Child1 && child.field1 == 1 -> child.field1
    !is val _ : Child2 -> 10
    is val child && child.field2 == 2 -> child.field2
    else -> 20
}

fun box(): String {
    assertEquals(foo(Child1(1)), 1)
    assertEquals(foo(Child1(2)), 2)
    assertEquals(foo(Child1(3)), 3)
    assertEquals(foo(Child2(2)), 2)
    assertEquals(foo(Child2(1)), 1)
    assertEquals(foo(Child2(3)), 3)

    assertEquals(foo2(Child1(1)), 1)
    assertEquals(foo2(Child1(2)), 2)
    assertEquals(foo2(Child1(3)), 3)
    assertEquals(foo2(Child2(2)), 2)
    assertEquals(foo2(Child2(1)), 1)
    assertEquals(foo2(Child2(3)), 3)

    assertEquals(foo3(Child1(1)), 1)
    assertEquals(foo3(Child1(2)), 10)
    assertEquals(foo3(Child1(3)), 10)
    assertEquals(foo3(Child2(2)), 2)
    assertEquals(foo3(Child2(1)), 20)
    assertEquals(foo3(Child2(3)), 20)
    return "OK"
}
