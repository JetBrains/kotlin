// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertNotNull

fun box(): String {
    assertNotNull({}.reflect())
    assertNotNull(Array(1) { {} }.single().reflect())

    return "OK"
}