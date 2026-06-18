// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class Holder(val id: Id = Id("OK"))

class NoDefault(val id: Id)

fun box(): String {
    val ctor = Holder::class.primaryConstructor!!
    if (ctor.callBy(emptyMap()).id.value != "OK") return "FAIL 1: ${ctor.callBy(emptyMap()).id.value}"
    if (ctor.callBy(mapOf(ctor.parameters.single() to Id("OK"))).id.value != "OK") return "FAIL 2: ${ctor.callBy(mapOf(ctor.parameters.single() to Id("OK"))).id.value}"
    val ndCtor = NoDefault::class.primaryConstructor!!
    return ndCtor.callBy(mapOf(ndCtor.parameters.single() to Id("OK"))).id.value
}
