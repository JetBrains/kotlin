// IGNORE_BACKEND: JS, JS_IR, JVM_IR, NATIVE
// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

// --

sealed class SealedClassWithTopLevelSubclasses {
    class NotASealedSubclass : TL2()
}
object TL1 : SealedClassWithTopLevelSubclasses()
open class TL2 : SealedClassWithTopLevelSubclasses()

// --

sealed class SealedClassWithNestedSubclasses {
    data class N1(val x: Unit) : SealedClassWithNestedSubclasses()
    object N2 : SealedClassWithNestedSubclasses()
}

// --

sealed class SealedClassWithNoSubclasses

// --

fun sealedSubclassNames(c: KClass<*>) = c.sealedSubclasses.map { it.simpleName ?: throw AssertionError("Unnamed class: ${it.java}") }.sorted()

fun box(): String {
    assertEquals(listOf("TL1", "TL2"), sealedSubclassNames(SealedClassWithTopLevelSubclasses::class))
    assertEquals(listOf("N1", "N2"), sealedSubclassNames(SealedClassWithNestedSubclasses::class))
    assertEquals(emptyList(), sealedSubclassNames(SealedClassWithNoSubclasses::class))

    assertEquals(emptyList(), sealedSubclassNames(String::class))
    assertEquals(emptyList(), sealedSubclassNames(Thread::class))
    assertEquals(emptyList(), sealedSubclassNames(FloatArray::class))

    return "OK"
}
