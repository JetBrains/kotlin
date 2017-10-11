package runtime.basic.standard

import kotlin.test.*

class Foo(val bar: Int)

fun <T> assertEquals(actual: T, expected: T) {
    if (actual != expected) throw AssertionError("Assertion failed. Expected value: $expected, actual value: $actual")
}

@Test fun runTest() {
    try {
        TODO()
        throw AssertionError("TODO() doesn't throw an exception")
    } catch(e: NotImplementedError) {}

    val foo = Foo(42)
    assertEquals(run { 42 }, 42)
    assertEquals(foo.run { bar }, 42)
    assertEquals(with(foo) { bar }, 42)
    assertEquals(foo.apply { bar }, foo)
    assertEquals(foo.also { it.bar }, foo)
    assertEquals(foo.let { it.bar }, 42)
    var i = 0
    repeat(10) { i++ }
    assertEquals(i, 10)
}