// TARGET_BACKEND: JVM
// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.test.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class I<T: Int>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class JLI<T: java.lang.Integer>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class U<T: Unit?>(val x: T)

OPTIONAL_JVM_INLINE_ANNOTATION
value class U2<T: Unit>(val x: T?)

OPTIONAL_JVM_INLINE_ANNOTATION
value class N<T: Nothing?>(val x: T)

val icUnit = U(Unit)
val icUnit2 = U2(Unit)
val icNull = N(null)

val anyIcUnit: Any = icUnit
val anyIcUnit2: Any = icUnit2
val anyIcNull: Any = icNull

val z = I(42)
val jli = JLI(java.lang.Integer(42))

fun box(): String {
    assertEquals(null, icUnit::class.javaPrimitiveType)
    assertEquals(null, icUnit2::class.javaPrimitiveType)
    assertEquals(null, icNull::class.javaPrimitiveType)
    assertEquals(null, anyIcUnit::class.javaPrimitiveType)
    assertEquals(null, anyIcUnit2::class.javaPrimitiveType)
    assertEquals(null, anyIcNull::class.javaPrimitiveType)
    assertEquals(null, z::class.javaPrimitiveType)
    assertEquals(null, jli::class.javaPrimitiveType)

    assertEquals(null, U::class.javaPrimitiveType)
    assertEquals(null, U2::class.javaPrimitiveType)
    assertEquals(null, N::class.javaPrimitiveType)
    assertEquals(null, I::class.javaPrimitiveType)
    assertEquals(null, JLI::class.javaPrimitiveType)

    return "OK"
}
