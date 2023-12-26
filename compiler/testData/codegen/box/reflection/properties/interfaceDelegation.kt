// TARGET_BACKEND: JVM
// WITH_REFLECT
// JVM_ABI_K1_K2_DIFF: KT-63828

import kotlin.reflect.*
import kotlin.reflect.full.*

interface Base {
    val message: String
}

class C(val base: Base) : Base by base

fun box(): String {
    val prop = C::class.memberProperties.single { it.name == "message" } as KProperty1<C, String>

    val c = C(object : Base {
        override val message: String = "OK"
    })

    return prop.get(c)
}
