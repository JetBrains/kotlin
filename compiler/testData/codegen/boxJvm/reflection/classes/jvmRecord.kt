// TARGET_BACKEND: JVM
// WITH_REFLECT
// FULL_JDK

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*
import kotlin.test.*

@JvmRecord
data class RecordPoint(val x: Double, val y: Double)

@JvmRecord
data class RecordPerson(val id: Int, val name: String, val active: Boolean = true)

@JvmRecord
data class RecordGeneric<T>(val value: T)

fun box(): String {
    // Basic class flags: @JvmRecord data class is a final data class
    assertTrue(RecordPoint::class.isData)
    assertTrue(RecordPoint::class.isFinal)
    assertFalse(RecordPoint::class.isOpen)
    assertFalse(RecordPoint::class.isAbstract)
    assertFalse(RecordPoint::class.isValue)
    assertFalse(RecordPoint::class.isSealed)

    // The JVM backing class is a java.lang.Record
    assertTrue(RecordPoint::class.java.superclass == java.lang.Record::class.java,
        "Expected java.lang.Record superclass, got: ${RecordPoint::class.java.superclass}")

    // jvmName is the simple binary name
    assertTrue(RecordPoint::class.jvmName.endsWith("RecordPoint"),
        "jvmName=${RecordPoint::class.jvmName}")

    // Primary constructor contains all record components, in declaration order
    val ctor = RecordPoint::class.primaryConstructor
    assertNotNull(ctor, "Expected primary constructor")
    assertEquals(2, ctor.parameters.size)
    assertEquals("x", ctor.parameters[0].name)
    assertEquals(0, ctor.parameters[0].index)
    assertEquals("y", ctor.parameters[1].name)
    assertEquals(1, ctor.parameters[1].index)
    assertFalse(ctor.parameters[0].isOptional)
    assertFalse(ctor.parameters[1].isOptional)

    // Calling the primary constructor via reflection produces the correct instance
    val p = ctor.call(1.0, 2.0)
    assertEquals(RecordPoint(1.0, 2.0), p)

    // Member properties correspond to the record components
    val propNames = RecordPoint::class.memberProperties.map { it.name }.toSet()
    assertTrue(propNames.containsAll(setOf("x", "y")),
        "Expected x,y in memberProperties, got $propNames")

    // Data class functions are present: copy, componentN, equals, hashCode, toString
    val funNames = RecordPoint::class.memberFunctions.map { it.name }.toSet()
    assertTrue("copy" in funNames, "Expected copy in memberFunctions")
    assertTrue("component1" in funNames, "Expected component1 in memberFunctions")
    assertTrue("component2" in funNames, "Expected component2 in memberFunctions")

    // Records with default parameters: the optional flag is set correctly
    val personCtor = RecordPerson::class.primaryConstructor
    assertNotNull(personCtor)
    assertEquals(3, personCtor.parameters.size)
    assertFalse(personCtor.parameters[0].isOptional)
    assertFalse(personCtor.parameters[1].isOptional)
    assertTrue(personCtor.parameters[2].isOptional)

    // callBy with only required args works
    val person = personCtor.callBy(mapOf(
        personCtor.parameters[0] to 42,
        personCtor.parameters[1] to "Alice"
    ))
    assertEquals(42, person.id)
    assertEquals("Alice", person.name)
    assertTrue(person.active) // default

    // Generic @JvmRecord
    assertTrue(RecordGeneric::class.isData)
    assertEquals(1, RecordGeneric::class.typeParameters.size)
    val genericCtor = RecordGeneric::class.primaryConstructor
    assertNotNull(genericCtor)
    assertEquals(1, genericCtor.parameters.size)
    assertEquals("value", genericCtor.parameters[0].name)

    return "OK"
}
