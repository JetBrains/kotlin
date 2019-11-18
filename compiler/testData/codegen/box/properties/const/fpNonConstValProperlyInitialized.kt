// !LANGUAGE: +NoConstantValueAttributeForNonConstVals
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

import kotlin.test.assertEquals

val minus0F = -0.0F
val minus0D = -0.0

fun box(): String {
    assertEquals(-0.0F, minus0F)
    assertEquals(-0.0, minus0D)

    return "OK"
}