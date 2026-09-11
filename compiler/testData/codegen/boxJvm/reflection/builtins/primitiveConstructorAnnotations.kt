// TARGET_BACKEND: JVM
// WITH_REFLECT
// Tests that annotations on primitive-type constructors are accessible and empty.

import kotlin.reflect.full.*
import kotlin.test.*

fun box(): String {
    for (klass in listOf(
        Int::class, Long::class, Short::class, Byte::class,
        Float::class, Double::class, Boolean::class, Char::class
    )) {
        val ctor = klass.constructors.single()
        assertEquals(emptyList(), ctor.annotations,
            "${klass.simpleName} constructor should have no annotations")
    }
    return "OK"
}
