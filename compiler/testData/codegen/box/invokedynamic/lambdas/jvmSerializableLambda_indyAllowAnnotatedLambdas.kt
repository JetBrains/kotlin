// LANGUAGE: +LightweightLambdas
// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// JVM_INDY_ALLOW_ANNOTATED_LAMBDAS
// FULL_JDK
// WITH_STDLIB

import kotlin.test.assertTrue

fun box(): String {
    assertTrue((@kotlin.jvm.JvmSerializableLambda {}) is java.io.Serializable)

    return "OK"
}
