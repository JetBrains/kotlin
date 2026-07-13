// WITH_REFLECT
// TARGET_BACKEND: JVM

@file:OptIn(ExperimentalStdlibApi::class)

package test

import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

@JvmInline
value class Id @JvmExposeBoxed constructor(val value: String)

@JvmInline
value class NonExposed(val value: String)

class Holder @JvmExposeBoxed constructor(id: Id)

class NonExposedHolder(id: Id)

fun box(): String {
    // Keep reflection behavior the same for exposed and non-exposed inline constructors.
    val idCtor = Id::class.primaryConstructor!!.javaConstructor
    if (idCtor.toString() != "null") return "FAIL 1: $idCtor"

    val nonExposedCtor = NonExposed::class.primaryConstructor!!.javaConstructor
    if (nonExposedCtor.toString() != "null") return "FAIL 2: $nonExposedCtor"

    // For ordinary class constructors we still use non-exposed one,
    // since this is the constructor, which is called from Kotlin code
    val holderCtor = Holder::class.primaryConstructor!!.javaConstructor
    if (holderCtor.toString() != "public test.Holder(java.lang.String,kotlin.jvm.internal.BoxingConstructorMarker,kotlin.jvm.internal.DefaultConstructorMarker)") return "FAIL 3: $holderCtor"
    val neHolderCtor = NonExposedHolder::class.primaryConstructor!!.javaConstructor
    if (neHolderCtor.toString() != "public test.NonExposedHolder(java.lang.String,kotlin.jvm.internal.DefaultConstructorMarker)") return "FAIL 4: $neHolderCtor"
    return "OK"
}
