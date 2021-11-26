// TARGET_BACKEND: JVM
// WITH_STDLIB
import kotlin.test.*

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class I(val x: Int)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class JLI(val x: java.lang.Integer)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class U(val x: Unit?)
@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
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
