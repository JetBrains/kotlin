// TARGET_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// API_VERSION: 2.2

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

import kotlin.coroutines.jvm.internal.*

val c = suspend {}

fun box(): String {
    val annotation = (c as BaseContinuationImpl).javaClass.getAnnotation(DebugMetadata::class.java)
    if (annotation.version != 1) return "FAIL ${annotation.version}"
    return "OK"
}
