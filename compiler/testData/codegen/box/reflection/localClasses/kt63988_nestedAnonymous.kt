// TARGET_BACKEND: JVM_IR
// WITH_REFLECT

import kotlin.reflect.full.memberProperties

class A {
    val prop = object {
        val nestedProp = object {}
    }
}

fun box() = if (A().prop::class.memberProperties.size == 1) "OK" else "Fail"