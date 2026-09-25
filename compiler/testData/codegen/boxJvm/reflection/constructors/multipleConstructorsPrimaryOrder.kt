// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that when a class has multiple constructors, primaryConstructor correctly
// identifies the source-declared primary constructor (not some arbitrary secondary one).

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

class MultiCtorClass(val x: Int, val y: String) {
    constructor(x: Int)          : this(x, "")
    constructor()                : this(0, "")
    constructor(y: String)       : this(0, y)
    constructor(x: Int, y: String, extra: Boolean) : this(x, if (extra) y.uppercase() else y)
}

class SingleCtorClass(val id: Int)

class NoArgClass {
    val value = 42
}

fun box(): String {
    // The primary constructor of MultiCtorClass is (Int, String)
    val primary = MultiCtorClass::class.primaryConstructor
        ?: return "Fail: MultiCtorClass has no primaryConstructor"

    val primaryParamNames = primary.parameters.map { it.name }
    assertEquals(listOf("x", "y"), primaryParamNames,
        "primaryConstructor should be (x: Int, y: String), got params: $primaryParamNames")

    // The primary constructor should be findable in the constructors list
    val ctors = MultiCtorClass::class.constructors.toList()
    assertEquals(5, ctors.size, "Expected 5 constructors")
    assertTrue(primary in ctors, "primaryConstructor must be in constructors list")

    // Calling the primary constructor via reflection should work
    val instance = primary.call(7, "hello")
    assertEquals(7, instance.x)
    assertEquals("hello", instance.y)

    // callBy with only the required params (no defaults here) also works
    val instance2 = primary.callBy(mapOf(
        primary.parameters[0] to 3,
        primary.parameters[1] to "world"
    ))
    assertEquals(3, instance2.x)
    assertEquals("world", instance2.y)

    // SingleCtorClass: only one constructor, which is the primary
    val singlePrimary = SingleCtorClass::class.primaryConstructor
        ?: return "Fail: SingleCtorClass has no primaryConstructor"
    assertEquals(listOf("id"), singlePrimary.parameters.map { it.name })

    // NoArgClass: primary constructor has zero parameters
    val noArgPrimary = NoArgClass::class.primaryConstructor
        ?: return "Fail: NoArgClass has no primaryConstructor"
    assertEquals(0, noArgPrimary.parameters.size)

    return "OK"
}
