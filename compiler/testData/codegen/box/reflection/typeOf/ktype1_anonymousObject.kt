// DONT_TARGET_EXACT_BACKEND: JVM_IR
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// ^ In Kotlin/JVM, KType.toString() of an anonymous object returns a synthetic name, not "???".
// WITH_STDLIB
// WITH_REFLECT

// FILE: lib.kt
import kotlin.reflect.*

inline fun <reified R> kType() = typeOf<R>()

inline fun <reified R> kType(obj: R) = kType<R>()

// FILE: main.kt
import kotlin.test.*
fun box(): String {
    val obj = object {}
    val objType = kType(obj)

    assertEquals("???", objType.toString())
    assertEquals(obj::class, objType.classifier)

    assertTrue(objType.arguments.isEmpty())
    assertFalse(objType.isMarkedNullable)

    return "OK"
}
