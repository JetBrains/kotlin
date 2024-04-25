// LANGUAGE: +LightweightLambdas
// OPT_IN: kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// WITH_REFLECT

import kotlin.reflect.jvm.reflect
import kotlin.test.*
import kotlin.jvm.JvmSerializableLambda

fun box(): String {
    assertNull({}.reflect())
    assertNull((fun () {}).reflect())
    assertNull((fun Any.() {}).reflect())

    assertNotNull((@JvmSerializableLambda {}).reflect())
    assertNotNull((@JvmSerializableLambda fun () {}).reflect())
    assertNotNull((@JvmSerializableLambda fun Any.() {}).reflect())

    return "OK"
}
