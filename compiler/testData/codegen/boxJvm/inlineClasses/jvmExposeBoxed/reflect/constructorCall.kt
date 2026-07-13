// WITH_REFLECT
// TARGET_BACKEND: JVM

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class Holder @JvmExposeBoxed constructor(val id: Id)

fun box(): String {
    return Holder::class.primaryConstructor!!.call(Id("OK")).id.value
}
