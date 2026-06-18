// WITH_REFLECT
// TARGET_BACKEND: JVM
// JVM_EXPOSE_BOXED

package test

import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

@JvmInline
value class Id(val value: String)

class Holder(id: Id)

fun box(): String {
    // Keep reflection behavior the same for exposed and non-exposed inline constructors.
    val idCtor = Id::class.primaryConstructor!!.javaConstructor
    if (idCtor.toString() != "null") return "FAIL 1: $idCtor"

    // For ordinary class constructors we still use non-exposed one,
    // since this is the constructor, which is called from Kotlin code
    val holderCtor = Holder::class.primaryConstructor!!.javaConstructor
    if (holderCtor.toString() != "public test.Holder(java.lang.String,kotlin.jvm.internal.BoxingConstructorMarker,kotlin.jvm.internal.DefaultConstructorMarker)") return "FAIL 3: $holderCtor"
    return "OK"
}
