// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

enum class Color { RED, GREEN, BLUE }

enum class Planet(val mass: Double, val radius: Double) {
    MERCURY(3.303e+23, 2.4397e6),
    VENUS(4.869e+24, 6.0518e6),
    EARTH(5.976e+24, 6.37814e6);
}

enum class Singleton { ONLY }

private inline fun <reified E : Enum<E>> checkEntriesProperty(enumClass: KClass<E>, expectedCount: Int) {
    val prop = enumClass.members.single { it.name == "entries" }
        .also { assertTrue(it is KProperty0<*>, "entries should be KProperty0") } as KProperty0<*>

    // Name
    assertEquals("entries", prop.name)

    // Parameters: none (static property, no receivers)
    assertEquals(emptyList(), prop.parameters)
    assertEquals(emptyList(), prop.typeParameters)

    // Return type is EnumEntries<E>
    assertEquals("kotlin.enums.EnumEntries<${enumClass.qualifiedName}>", prop.returnType.toString())

    // Visibility and modifiers
    assertEquals(KVisibility.PUBLIC, prop.visibility)
    assertTrue(prop.isFinal)
    assertFalse(prop.isOpen)
    assertFalse(prop.isAbstract)
    assertFalse(prop.isSuspend)
    assertFalse(prop.isLateinit)
    assertFalse(prop.isConst)

    // Not mutable
    assertFalse(prop is KMutableProperty0<*>)

    // No delegate
    assertNull(prop.getDelegate())

    // Getter
    val getter = prop.getter
    assertEquals("<get-entries>", getter.name)
    assertEquals(emptyList(), getter.parameters)
    assertEquals("kotlin.enums.EnumEntries<${enumClass.qualifiedName}>", getter.returnType.toString())
    assertEquals(KVisibility.PUBLIC, getter.visibility)
    assertTrue(getter.isFinal)
    assertFalse(getter.isSuspend)
    assertFalse(getter.isInline)
    assertFalse(getter.isExternal)
    assertFalse(getter.isOperator)
    assertFalse(getter.isInfix)

    // Back-reference from getter to property
    assertEquals(prop, getter.property)

    // Calling via various paths all returns the same result
    val expectedEntries = enumEntries<E>()
    assertEquals(expectedCount, expectedEntries.size)
    assertEquals(expectedEntries, prop.get())
    assertEquals(expectedEntries, prop.invoke())
    assertEquals(expectedEntries, prop.call())
    assertEquals(expectedEntries, prop.callBy(emptyMap()))
    assertEquals(expectedEntries, getter())
    assertEquals(expectedEntries, getter.call())
    assertEquals(expectedEntries, getter.callBy(emptyMap()))
}

private fun assertAreEqual(a: Any, b: Any) {
    assertEquals(a, b)
    assertEquals(b, a)
    assertEquals(a.hashCode(), b.hashCode())
}

fun box(): String {
    checkEntriesProperty(Color::class, 3)
    checkEntriesProperty(Planet::class, 3)
    checkEntriesProperty(Singleton::class, 1)

    // Callable reference form
    val colorRef = Color::entries
    assertEquals("val entries: kotlin.enums.EnumEntries<Color>", colorRef.toString())

    // Reference from members matches callable reference
    val colorFromMembers = Color::class.members.single { it.name == "entries" } as KProperty0<*>
    assertAreEqual(colorRef, colorFromMembers)

    // Contents of the entries list
    val colors = Color::entries.call()
    assertEquals(listOf(Color.RED, Color.GREEN, Color.BLUE), colors)

    val planets = Planet::entries.call()
    assertEquals(Planet.MERCURY, planets.first())
    assertEquals(3, planets.size)

    return "OK"
}
