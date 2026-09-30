// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

sealed interface JsonElement
data class JsonString(val value: String) : JsonElement
data class JsonNumber(val value: Double) : JsonElement
data class JsonArray(val items: List<JsonElement>) : JsonElement
data object JsonNull : JsonElement

sealed interface Either<out L, out R>
data class Left<out L>(val value: L) : Either<L, Nothing>
data class Right<out R>(val value: R) : Either<Nothing, R>

fun box(): String {
    // Sealed interface itself: sealed and an interface
    assertTrue(JsonElement::class.isSealed)
    assertTrue(JsonElement::class.java.isInterface)

    // Sealed interface has no supertypes other than Any (no explicit superinterface)
    val jsonSupertypes = JsonElement::class.supertypes
    assertEquals(1, jsonSupertypes.size, "JsonElement should only have Any as supertype")
    assertEquals("kotlin.Any", jsonSupertypes.single().toString())

    // Implementations have JsonElement in their supertypes
    assertEquals("[JsonElement, kotlin.Any]", JsonString::class.supertypes.toString())

    // allSupertypes for an implementation includes the interface chain
    val stringAllSupertypes = JsonString::class.allSupertypes.map { it.toString() }
    assertTrue(stringAllSupertypes.any { it.contains("JsonElement") })
    assertTrue(stringAllSupertypes.any { it.contains("Any") })

    // data object implementing sealed interface
    assertEquals("[JsonElement, kotlin.Any]", JsonNull::class.supertypes.toString())

    // Generic sealed interface: Either<L, R>
    assertTrue(Either::class.isSealed)
    assertTrue(Either::class.java.isInterface)
    assertEquals(2, Either::class.typeParameters.size)
    assertEquals("L", Either::class.typeParameters[0].name)
    assertEquals(KVariance.OUT, Either::class.typeParameters[0].variance)
    assertEquals("R", Either::class.typeParameters[1].name)
    assertEquals(KVariance.OUT, Either::class.typeParameters[1].variance)

    // Left<L> implements Either<L, Nothing>, and the type argument refers to Left's own type parameter
    val eitherSupertype = Left::class.supertypes.first()
    assertEquals("Either<L, kotlin.Nothing>", eitherSupertype.toString())
    assertEquals(Left::class.typeParameters.single(), eitherSupertype.arguments[0].type!!.classifier)
    assertEquals("[Either<kotlin.Nothing, R>, kotlin.Any]", Right::class.supertypes.toString())

    // sealedSubclasses of JsonElement
    val subs = JsonElement::class.sealedSubclasses
    assertEquals(4, subs.size)
    val subNames = subs.map { it.simpleName.orEmpty() }.sorted()
    assertEquals(listOf("JsonArray", "JsonNull", "JsonNumber", "JsonString"), subNames)

    // superclasses (KClass not KType form)
    val strSuperclasses = JsonString::class.superclasses
    assertTrue(strSuperclasses.any { it == JsonElement::class },
        "JsonString.superclasses should include JsonElement")
    assertTrue(strSuperclasses.any { it == Any::class })

    // isSubclassOf
    assertTrue(JsonString::class.isSubclassOf(JsonElement::class))
    assertTrue(JsonString::class.isSubclassOf(Any::class))
    assertFalse(JsonString::class.isSubclassOf(JsonNumber::class))

    // isSuperclassOf
    assertTrue(JsonElement::class.isSuperclassOf(JsonString::class))
    assertFalse(JsonNumber::class.isSuperclassOf(JsonString::class))

    return "OK"
}
