package kotlin.test.tests

import kotlin.test.*

class A {
    @Test
    fun foo1() {}

    @Test
    fun foo2() {}

    @Test
    fun bar() {}
}

class B {
    @Test
    fun foo1() {}

    @Test
    fun foo2() {}

    @Test
    fun bar() {}
}

@Test
fun foo1() {}

@Test
fun foo2() {}

@Test
fun bar() {}