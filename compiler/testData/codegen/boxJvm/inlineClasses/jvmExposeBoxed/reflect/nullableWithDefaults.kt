// WITH_REFLECT
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// ISSUE: KT-86525
// Reflecting over a class whose exposed constructor takes a nullable value class throws
// KotlinReflectionInternalError: "Inconsistent number of parameters in the descriptor and the Java reflection
// object", because the 'DefaultConstructorMarker' overload the metadata describes is no longer emitted.
// TODO: Remove if green after the fix

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class NullableWithDefault @JvmExposeBoxed constructor(val id: Id? = Id("OK"))

fun box(): String {
    val ctor = NullableWithDefault::class.primaryConstructor!!
    if (ctor.callBy(emptyMap()).id?.value != "OK") return "FAIL 1"
    if (ctor.callBy(mapOf(ctor.parameters.single() to Id("OK"))).id?.value != "OK") return "FAIL 2"
    return "OK"
}
