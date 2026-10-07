// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

const val const = "const"
val nonConst = "nonConst"
lateinit var topLevelLateinit: String

class A {
    lateinit var lateinit: Unit
    var nonLateinit = Unit
}

object Obj {
    const val objectConst = "objectConst"
    @JvmField val objectJvmField = "objectJvmField"
}

class WithCompanion {
    companion object {
        const val companionConst = "companionConst"
        lateinit var companionLateinit: String
    }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> KClass<T>.property(name: String): KProperty1<T, *> = members.single { it.name == name } as KProperty1<T, *>

fun box(): String {
    assertTrue(::const.isConst)
    assertEquals("const", ::const.call())
    assertFalse(::nonConst.isConst)

    assertTrue(A::lateinit.isLateinit)
    assertFalse(A::nonLateinit.isLateinit)

    assertTrue(::topLevelLateinit.isLateinit)
    ::topLevelLateinit.set("topLevelLateinit")
    assertEquals("topLevelLateinit", ::topLevelLateinit.get())

    WithCompanion.Companion::class.property("companionConst").let {
        assertTrue(it.isConst)
        assertEquals("companionConst", it.get(WithCompanion.Companion))
    }
    (WithCompanion.Companion::class.property("companionLateinit") as KMutableProperty1<WithCompanion.Companion, String>).let {
        assertTrue(it.isLateinit)
        it.set(WithCompanion.Companion, "companionLateinit")
        assertEquals("companionLateinit", it.get(WithCompanion.Companion))
    }

    assertTrue(Obj::objectConst.isConst)
    assertEquals("objectConst", Obj::objectConst.get())
    assertEquals("objectJvmField", Obj::objectJvmField.get())

    // TODO: this is a bug. Unbound `const val` and `@JvmField val` members of an object have an instance parameter, but their caller
    //  expects no arguments, because the underlying field is static. So they can't be called with the object instance, unlike other
    //  members of objects and unlike the same properties in companion objects. Replace with calls with `Obj` once fixed.
    for ((name, value) in listOf("objectConst" to "objectConst", "objectJvmField" to "objectJvmField")) {
        val property = Obj::class.property(name)
        assertEquals(listOf(Obj::class), property.parameters.map { it.type.classifier })
        assertFailsWith<IllegalArgumentException> { property.get(Obj) }
        assertFailsWith<IllegalArgumentException> { property.getter.call(Obj) }
        assertEquals(value, property.call())
    }

    return "OK"
}
