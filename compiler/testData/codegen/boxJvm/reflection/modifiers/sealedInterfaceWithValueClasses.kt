// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

sealed interface Measurement {
    @JvmInline value class Distance(val meters: Double) : Measurement
    @JvmInline value class Weight(val grams: Double) : Measurement
    @JvmInline value class Duration(val millis: Long) : Measurement
    data class Combined(val d: Double, val w: Double) : Measurement
    data object Empty : Measurement
}

sealed interface Token
@JvmInline value class IntToken(val value: Int) : Token
@JvmInline value class StringToken(val value: String) : Token
data object NullToken : Token

fun box(): String {
    // Sealed interface itself
    assertTrue(Measurement::class.isSealed)
    assertTrue(Measurement::class.java.isInterface)
    assertFalse(Measurement::class.isAbstract)
    assertFalse(Measurement::class.isFinal)

    // sealedSubclasses contains all 5 variants
    val subclasses = Measurement::class.sealedSubclasses
    assertEquals(5, subclasses.size)
    val names = subclasses.map { it.simpleName }.sorted()
    assertEquals(listOf("Combined", "Distance", "Duration", "Empty", "Weight"), names)

    // Value class variants: isValue=true, isData=false
    val distance = subclasses.first { it.simpleName == "Distance" }
    assertTrue(distance.isValue)
    assertFalse(distance.isData)
    assertFalse(distance.isAbstract)
    assertTrue(distance.isFinal)

    val weight = subclasses.first { it.simpleName == "Weight" }
    assertTrue(weight.isValue)

    val duration = subclasses.first { it.simpleName == "Duration" }
    assertTrue(duration.isValue)

    // Data class variant: isData=true, isValue=false
    val combined = subclasses.first { it.simpleName == "Combined" }
    assertFalse(combined.isValue)
    assertTrue(combined.isData)
    assertFalse(combined.isAbstract)

    // Data object variant: isData=true, isObject (objectInstance not null)
    val empty = subclasses.first { it.simpleName == "Empty" }
    assertFalse(empty.isValue)
    assertTrue(empty.isData)
    assertNotNull(empty.objectInstance)
    assertEquals(Measurement.Empty, empty.objectInstance)

    // Top-level sealed interface with external value class variants
    assertTrue(Token::class.isSealed)
    val tokenSubs = Token::class.sealedSubclasses
    assertEquals(3, tokenSubs.size)

    val intToken = tokenSubs.first { it.simpleName == "IntToken" }
    assertTrue(intToken.isValue)
    val strToken = tokenSubs.first { it.simpleName == "StringToken" }
    assertTrue(strToken.isValue)
    val nullToken = tokenSubs.first { it.simpleName == "NullToken" }
    assertFalse(nullToken.isValue)
    assertTrue(nullToken.isData)
    assertNotNull(nullToken.objectInstance)

    // Value class in sealed interface: primary constructor has the underlying value
    val distanceCtor = distance.primaryConstructor
    assertNotNull(distanceCtor)
    assertEquals(1, distanceCtor.parameters.size)
    assertEquals("meters", distanceCtor.parameters[0].name)

    // Instantiation via reflection
    val d = distanceCtor.call(5.0)
    assertEquals(Measurement.Distance(5.0), d)

    return "OK"
}
