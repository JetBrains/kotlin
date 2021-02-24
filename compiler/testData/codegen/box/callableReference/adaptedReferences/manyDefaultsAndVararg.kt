// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// WITH_RUNTIME

import kotlin.test.assertEquals

object E : Exception()

fun foo(
        a: String = "fail",
        b: Int,
        c: Double = 3.14,
        vararg d: String,
        e: Throwable = E
) {
    assertEquals("ok", a)
    assertEquals(42, b)
    assertEquals(3.14, c)
    assertEquals(0, d.size)
    assertEquals(E, e)
}

fun bar(f: (String, Int) -> Unit) = f("ok", 42)

fun box(): String {
    bar(::foo)
    return "OK"
}
