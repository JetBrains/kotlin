// TARGET_BACKEND: JVM
// WITH_REFLECT

// Members of built-in classes obtained via `KClass.members` must be not only enumerable (see primitiveClassMembers.kt,
// KT-88944), but also callable, including members that are mapped to methods of java.lang classes.

import kotlin.reflect.*
import kotlin.test.assertEquals

private fun KClass<*>.member(name: String, vararg valueParameterTypes: KClass<*>): KCallable<*> =
    members.single { member ->
        member.name == name &&
                member.parameters.filter { it.kind == KParameter.Kind.VALUE }.map { it.type.classifier } == valueParameterTypes.toList()
    }

enum class E { X, Y }

fun box(): String {
    assertEquals(3, String::class.member("length").call("abc"))
    assertEquals("bc", String::class.member("subSequence", Int::class, Int::class).call("abc", 1, 3))
    assertEquals(-1, String::class.member("compareTo", String::class).call("a", "b"))
    assertEquals(2, CharSequence::class.member("length").call(StringBuilder("ab")))
    assertEquals(-1, Comparable::class.members.single { it.name == "compareTo" }.call(1, 2))

    assertEquals("Y", Enum::class.member("name").call(E.Y))
    assertEquals(1, E::class.member("ordinal").call(E.Y))
    assertEquals(-1, E::class.member("compareTo", E::class).call(E.X, E.Y))
    assertEquals("m", Throwable::class.member("message").call(IllegalStateException("m")))

    assertEquals("OK", Any::class.member("toString").call("OK"))
    assertEquals(true, Any::class.member("equals", Any::class).call(E.X, E.X))
    assertEquals(5, List::class.members.single { it.name == "get" }.call(listOf(5), 0))
    assertEquals(1, Map::class.member("size").call(mapOf(1 to 2)))

    return "OK"
}
