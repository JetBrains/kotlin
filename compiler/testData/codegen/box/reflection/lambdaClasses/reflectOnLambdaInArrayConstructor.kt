// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertNotNull

fun box(): String {
    assertNotNull({}.reflect())
    assertNotNull(Array(1) { {} }.single().reflect())

    return "OK"
}