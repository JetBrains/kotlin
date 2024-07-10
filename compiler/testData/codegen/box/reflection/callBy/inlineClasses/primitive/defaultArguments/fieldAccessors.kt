// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value: Int) {
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

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

fun box(): String {
    val c = C()
    val zero = S(0)
    val one = S(1)
    val two = S(2)

    assertEquals(Unit, c.nonNullUnboundRef().setter.callBy(c, zero))
    assertEquals(zero, c.nonNullUnboundRef().callBy(c))
    assertEquals(zero, c.nonNullUnboundRef().getter.callBy(c))

    assertEquals(Unit, c.nonNullBoundRef().setter.callBy(one))
    assertEquals(one, c.nonNullBoundRef().callBy())
    assertEquals(one, c.nonNullBoundRef().getter.callBy())

    assertEquals(Unit, c.nullableUnboundRef().setter.callBy(c, zero))
    assertEquals(zero, c.nullableUnboundRef().callBy(c))
    assertEquals(zero, c.nullableUnboundRef().getter.callBy(c))

    assertEquals(Unit, c.nullableBoundRef().setter.callBy(one))
    assertEquals(one, c.nullableBoundRef().callBy())
    assertEquals(one, c.nullableBoundRef().getter.callBy())

    val nonNullTopLevel = ::nonNullTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nonNullTopLevel.setter.callBy(two))
    assertEquals(two, nonNullTopLevel.callBy())
    assertEquals(two, nonNullTopLevel.getter.callBy())

    val nullableTopLevel = ::nullableTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nullableTopLevel.setter.callBy(two))
    assertEquals(two, nullableTopLevel.callBy())
    assertEquals(two, nullableTopLevel.getter.callBy())

    return "OK"
}
