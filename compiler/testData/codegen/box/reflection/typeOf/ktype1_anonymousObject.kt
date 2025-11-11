// DONT_TARGET_EXACT_BACKEND: JVM_IR
// IGNORE_BACKEND_K2_MULTI_MODULE: JVM_IR, JVM_IR_SERIALIZE
// ^ In Kotlin/JVM, KType.toString() of an anonymous object returns a synthetic name, not "???".
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// ^^^ In 2.2.0 there was old value `(non-denotable type)`
//     In 2.3.0-Beta1, `Render non-denotable types as "???" on non-JVM targets` has been done.
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
