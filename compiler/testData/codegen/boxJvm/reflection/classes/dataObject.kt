// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

data object GlobalConfig
data object EmptyState : java.io.Serializable

interface Configurable
data object DefaultConfig : Configurable

fun box(): String {
    // data object has both isData=true and objectInstance≠null
    assertTrue(GlobalConfig::class.isData,
        "data object should have isData=true")
    assertNotNull(GlobalConfig::class.objectInstance,
        "data object should have non-null objectInstance")
    assertEquals(GlobalConfig, GlobalConfig::class.objectInstance)

    // data object is final and not sealed/abstract/open/inner/companion/value
    assertTrue(GlobalConfig::class.isFinal)
    assertFalse(GlobalConfig::class.isOpen)
    assertFalse(GlobalConfig::class.isAbstract)
    assertFalse(GlobalConfig::class.isSealed)
    assertFalse(GlobalConfig::class.isInner)
    assertFalse(GlobalConfig::class.isCompanion)
    assertFalse(GlobalConfig::class.isValue)

    // No primary constructor parameters (data objects have a private no-arg constructor)
    val ctor = GlobalConfig::class.primaryConstructor
    if (ctor != null) {
        assertEquals(0, ctor.parameters.size,
            "data object primary constructor should have no parameters")
    }

    // data object has toString, equals, hashCode — all override Object
    val fnNames = GlobalConfig::class.memberFunctions.map { it.name }.toSet()
    assertTrue("equals" in fnNames)
    assertTrue("hashCode" in fnNames)
    assertTrue("toString" in fnNames)

    // data object does NOT have copy() or componentN() functions
    assertFalse("copy" in fnNames, "data object should not have copy()")
    assertFalse("component1" in fnNames, "data object should not have component1()")

    // toString returns the object name by default
    assertEquals("GlobalConfig", GlobalConfig.toString())
    assertEquals("GlobalConfig", GlobalConfig::class.objectInstance.toString())

    // data object implementing an interface
    assertTrue(DefaultConfig::class.isData)
    assertNotNull(DefaultConfig::class.objectInstance)
    val supertypeNames = DefaultConfig::class.supertypes.map { it.toString() }
    assertTrue(supertypeNames.any { it.contains("Configurable") },
        "Expected Configurable in supertypes: $supertypeNames")

    // data object implementing Serializable
    assertTrue(EmptyState::class.isData)
    assertNotNull(EmptyState::class.objectInstance)
    val emptySupertypes = EmptyState::class.supertypes.map { it.toString() }
    assertTrue(emptySupertypes.any { it.contains("Serializable") },
        "Expected Serializable in supertypes: $emptySupertypes")

    return "OK"
}
