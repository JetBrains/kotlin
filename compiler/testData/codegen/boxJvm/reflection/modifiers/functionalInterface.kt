// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.test.*

fun interface StringMapper {
    fun map(s: String): String
}

fun interface BiMapper<A, B, R> {
    fun map(a: A, b: B): R
}

fun interface AsyncTask<T> {
    suspend fun run(): T
}

// Contrast: regular interface (not functional)
interface RegularInterface {
    fun doSomething(): Unit
}

// Abstract class — not a fun interface
abstract class AbstractFun {
    abstract fun invoke(): Unit
}

fun box(): String {
    // fun interface has isFun=true
    assertTrue(StringMapper::class.isFun,
        "StringMapper should have isFun=true")
    assertTrue(BiMapper::class.isFun,
        "BiMapper should have isFun=true")
    assertTrue(AsyncTask::class.isFun,
        "AsyncTask should have isFun=true")

    // fun interface is an interface
    assertTrue(StringMapper::class.java.isInterface)
    assertTrue(BiMapper::class.java.isInterface)
    assertTrue(AsyncTask::class.java.isInterface)

    // Regular interface does NOT have isFun=true
    assertFalse(RegularInterface::class.isFun,
        "RegularInterface should have isFun=false")

    // Abstract class does NOT have isFun=true
    assertFalse(AbstractFun::class.isFun,
        "AbstractFun class should have isFun=false")

    // fun interface other modifiers
    assertFalse(StringMapper::class.isAbstract)
    assertFalse(StringMapper::class.isSealed)
    assertFalse(StringMapper::class.isData)
    assertFalse(StringMapper::class.isValue)
    assertFalse(StringMapper::class.isFinal)
    assertFalse(StringMapper::class.isOpen)

    // The single abstract method is present in members
    val mapFn = StringMapper::class.members.firstOrNull { it.name == "map" }
    assertNotNull(mapFn, "Expected 'map' in StringMapper members")
    assertTrue(mapFn.isAbstract)

    // Generic fun interface has type parameters
    assertEquals(3, BiMapper::class.typeParameters.size)
    assertEquals(listOf("A", "B", "R"), BiMapper::class.typeParameters.map { it.name })

    // Suspend fun interface
    val runFn = AsyncTask::class.members.firstOrNull { it.name == "run" }
    assertNotNull(runFn, "Expected 'run' in AsyncTask members")
    assertTrue(runFn.isAbstract)
    assertTrue((runFn as KFunction<*>).isSuspend,
        "AsyncTask.run() should be suspend")

    // SAM conversion: a lambda can be passed as the fun interface
    val mapper: StringMapper = StringMapper { it.uppercase() }
    assertEquals("HELLO", mapper.map("hello"))

    // Reflective call on the single abstract method
    // via a concrete implementation
    val impl: StringMapper = StringMapper { "[$it]" }
    val mapRef = impl::map
    assertEquals("[test]", mapRef.invoke("test"))

    return "OK"
}
