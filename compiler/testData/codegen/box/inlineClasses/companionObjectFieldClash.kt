// ISSUE: KT-90210
// WORKS_WHEN_VALUE_CLASS

import kotlin.test.assertEquals

OPTIONAL_JVM_INLINE_ANNOTATION
value class A(val value: String) {
    companion object {
        val value = "companion"
    }
}

fun makeBoxedA(): Any = A("value")

fun box(): String {
    assertEquals("companion", A.value)
    assertEquals("abc", A("abc").value)
    assertEquals("A(value=value)", makeBoxedA().toString())
    return "OK"
}
