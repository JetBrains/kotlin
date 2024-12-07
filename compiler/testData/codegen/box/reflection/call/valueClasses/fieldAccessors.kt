// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value1: UInt, val value2: Int) {
    operator fun plus(other: S): S = S(this.value1 + other.value1, this.value2 + other.value2)
}

class C {
    private var nonNullMember: S = S(UInt.MAX_VALUE, -10)
    private var nullableMember: S? = S(UInt.MAX_VALUE, -10)

    fun nonNullUnboundRef() = C::nonNullMember.apply { isAccessible = true }
    fun nonNullBoundRef() = this::nonNullMember.apply { isAccessible = true }
    fun nullableUnboundRef() = C::nullableMember.apply { isAccessible = true }
    fun nullableBoundRef() = this::nullableMember.apply { isAccessible = true }
}

private var nonNullTopLevel: S = S(UInt.MAX_VALUE, -10)
private var nullableTopLevel: S? = S(UInt.MAX_VALUE, -10)

fun box(): String {
    val c = C()
    val zero = S(0U, 5)
    val one = S(1U, 10)
    val two = S(2U, 20)

    assertEquals(Unit, c.nonNullUnboundRef().setter.call(c, zero))
    assertEquals(zero, c.nonNullUnboundRef().call(c))
    assertEquals(zero, c.nonNullUnboundRef().getter.call(c))

    assertEquals(Unit, c.nonNullBoundRef().setter.call(one))
    assertEquals(one, c.nonNullBoundRef().call())
    assertEquals(one, c.nonNullBoundRef().getter.call())

    assertEquals(Unit, c.nullableUnboundRef().setter.call(c, zero))
    assertEquals(zero, c.nullableUnboundRef().call(c))
    assertEquals(zero, c.nullableUnboundRef().getter.call(c))

    assertEquals(Unit, c.nullableBoundRef().setter.call(one))
    assertEquals(one, c.nullableBoundRef().call())
    assertEquals(one, c.nullableBoundRef().getter.call())

    val nonNullTopLevel = ::nonNullTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nonNullTopLevel.setter.call(two))
    assertEquals(two, nonNullTopLevel.call())
    assertEquals(two, nonNullTopLevel.getter.call())

    val nullableTopLevel = ::nullableTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nullableTopLevel.setter.call(two))
    assertEquals(two, nullableTopLevel.call())
    assertEquals(two, nullableTopLevel.getter.call())

    return "OK"
}
