// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KClass
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

sealed interface SealedInterfaceWithValueClassSubclasses {
    @JvmInline value class V1(val x: Int) : SealedInterfaceWithValueClassSubclasses
    data object O1 : SealedInterfaceWithValueClassSubclasses
}
@JvmInline value class V2(val x: String) : SealedInterfaceWithValueClassSubclasses

// --

fun sealedSubclassNames(c: KClass<*>) = c.sealedSubclasses.map { it.simpleName ?: throw AssertionError("Unnamed class: ${it.java}") }.sorted()

fun box(): String {
    assertEquals(listOf("TL1", "TL2"), sealedSubclassNames(SealedClassWithTopLevelSubclasses::class))
    assertEquals(listOf("N1", "N2"), sealedSubclassNames(SealedClassWithNestedSubclasses::class))
    assertEquals(emptyList(), sealedSubclassNames(SealedClassWithNoSubclasses::class))
    assertEquals(listOf("O1", "V1", "V2"), sealedSubclassNames(SealedInterfaceWithValueClassSubclasses::class))

    assertEquals(emptyList(), sealedSubclassNames(String::class))
    assertEquals(emptyList(), sealedSubclassNames(Thread::class))
    assertEquals(emptyList(), sealedSubclassNames(FloatArray::class))

    return "OK"
}
