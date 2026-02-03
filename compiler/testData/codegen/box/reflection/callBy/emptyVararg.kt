// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals
import kotlin.test.assertFalse

fun f(vararg s: String): Array<out String> = s

class C(vararg val t: Int)

fun box(): String {
    assertFalse(::f.parameters.single().isOptional)
    assertEquals(emptyList(), ::f.callBy(emptyMap()).toList())
    assertFalse(::C.parameters.single().isOptional)
    assertEquals(emptyList(), ::C.callBy(emptyMap()).t.toList())
    return "OK"
}
