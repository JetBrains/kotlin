// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED

@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.reflect.full.primaryConstructor

@JvmInline
value class Id(val value: String)

class Outer {
    inner class Holder(val id: Id)
}

fun box(): String {
    return (Outer()::Holder).call(Id("OK")).id.value
}
