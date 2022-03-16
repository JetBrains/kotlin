// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

inline class S(val value: Int) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    private var nonNullMember: S = S(-1)
    private var nullableMember: S? = S(-1)

    fun nonNullUnboundRef() = C::nonNullMember.apply { isAccessible = true }
    fun nonNullBoundRef() = this::nonNullMember.apply { isAccessible = true }
    fun nullableUnboundRef() = C::nullableMember.apply { isAccessible = true }
    fun nullableBoundRef() = this::nullableMember.apply { isAccessible = true }
}

private var nonNullTopLevel: S = S(-1)
private var nullableTopLevel: S? = S(-1)

fun box(): String {
    val c = C()
    val zero = S(0)
    val one = S(1)
    val two = S(2)

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
