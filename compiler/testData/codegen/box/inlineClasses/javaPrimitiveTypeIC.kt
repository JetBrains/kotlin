// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class I(val x: Int)

OPTIONAL_JVM_INLINE_ANNOTATION
value class JLI(val x: java.lang.Integer)

OPTIONAL_JVM_INLINE_ANNOTATION
value class U(val x: Unit?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class N(val x: Nothing?)

val icUnit = U(Unit)
val icNull = N(null)

val anyIcUnit: Any = icUnit
val anyIcNull: Any = icNull

val z = I(42)
val jli = JLI(java.lang.Integer(42))

fun box(): String {
    assertEquals(null, icUnit::class.javaPrimitiveType)
    assertEquals(null, icNull::class.javaPrimitiveType)
    assertEquals(null, anyIcUnit::class.javaPrimitiveType)
    assertEquals(null, anyIcNull::class.javaPrimitiveType)
    assertEquals(null, z::class.javaPrimitiveType)
    assertEquals(null, jli::class.javaPrimitiveType)

    assertEquals(null, U::class.javaPrimitiveType)
    assertEquals(null, N::class.javaPrimitiveType)
    assertEquals(null, I::class.javaPrimitiveType)
    assertEquals(null, JLI::class.javaPrimitiveType)

    return "OK"
}
