// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED
// USE_LEGACY_REFLECTION_IMPLEMENTATION

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class Holder(val id: Id)

fun box(): String {
    return Holder::class.primaryConstructor!!.call(Id("OK")).id.value
}
