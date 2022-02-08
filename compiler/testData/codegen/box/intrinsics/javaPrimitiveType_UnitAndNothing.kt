// TARGET_BACKEND: JVM
// WITH_STDLIB
import kotlin.test.*

val pUnit = Unit
val pNUnit: Unit? = Unit

fun box(): String {
    assertEquals(null, pUnit::class.javaPrimitiveType)
    assertEquals(null, pNUnit!!::class.javaPrimitiveType)

    assertEquals(null, Unit::class.javaPrimitiveType)
    @Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR")
    assertEquals(java.lang.Void.TYPE, Nothing::class.javaPrimitiveType)

    return "OK"
}
