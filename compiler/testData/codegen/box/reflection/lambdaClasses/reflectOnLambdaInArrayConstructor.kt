// LAMBDAS: CLASS
// OPT_IN: kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.*
import kotlin.test.assertNotNull

fun box(): String {
    assertNotNull({}.reflect())
    assertNotNull(Array(1) { {} }.single().reflect())

    return "OK"
}
