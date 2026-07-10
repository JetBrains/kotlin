// TARGET_BACKEND: JVM
// WITH_REFLECT
// WITH_STDLIB

import kotlin.jvm.JvmExposeBoxed
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

@JvmInline @JvmExposeBoxed
value class ExposedInt(val raw: Int) {
    fun doubled(): Int = raw * 2
    operator fun plus(other: ExposedInt) = ExposedInt(raw + other.raw)
}

@JvmInline @JvmExposeBoxed
value class ExposedString(val raw: String) {
    val length: Int get() = raw.length
}

@JvmInline // no @JvmExposeBoxed — for comparison
value class UnexposedInt(val raw: Int) {
    fun doubled(): Int = raw * 2
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    // Both are value classes
    assertTrue(ExposedInt::class.isValue)
    assertTrue(ExposedString::class.isValue)
    assertTrue(UnexposedInt::class.isValue)

    // @JvmExposeBoxed exposes an additional public constructor that accepts the boxed wrapper
    val exposedCtors = ExposedInt::class.constructors
    val unexposedCtors = UnexposedInt::class.constructors
    assertTrue(exposedCtors.size > unexposedCtors.size,
        "@JvmExposeBoxed should expose additional constructor; exposed=${exposedCtors.size}, unexposed=${unexposedCtors.size}")

    // The primary constructor still takes the underlying raw type
    val primaryCtor = ExposedInt::class.primaryConstructor
    assertNotNull(primaryCtor, "Expected primary constructor")
    assertEquals(1, primaryCtor.parameters.size)
    assertEquals("raw", primaryCtor.parameters.single().name)

    // Member functions are accessible
    val doubled = ExposedInt::class.memberFunctions.firstOrNull { it.name == "doubled" }
    assertNotNull(doubled, "Expected 'doubled' in memberFunctions")
    assertFalse(doubled.isAbstract)

    val plusOp = ExposedInt::class.memberFunctions.firstOrNull { it.name == "plus" }
    assertNotNull(plusOp, "Expected 'plus' operator in memberFunctions")
    assertTrue(plusOp.isOperator)

    // ExposedString has a computed property
    val lengthProp = ExposedString::class.memberProperties.firstOrNull { it.name == "length" }
    assertNotNull(lengthProp, "Expected 'length' memberProperty")

    // The 'raw' backing property is present
    val rawProp = ExposedInt::class.memberProperties.firstOrNull { it.name == "raw" }
    assertNotNull(rawProp, "Expected 'raw' memberProperty")
    assertTrue(rawProp.isFinal)
    assertFalse(rawProp.isMutable)

    // jvmName reflects the value class
    assertTrue(ExposedInt::class.jvmName.endsWith("ExposedInt"))

    return "OK"
}
