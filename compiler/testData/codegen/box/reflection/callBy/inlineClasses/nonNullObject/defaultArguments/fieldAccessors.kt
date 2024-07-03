// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    private var nonNullMember: S = S("")
    private var nullableMember: S? = S("")

    fun nonNullUnboundRef() = C::nonNullMember.apply { isAccessible = true }
    fun nonNullBoundRef() = this::nonNullMember.apply { isAccessible = true }
    fun nullableUnboundRef() = C::nullableMember.apply { isAccessible = true }
    fun nullableBoundRef() = this::nullableMember.apply { isAccessible = true }
}

private var nonNullTopLevel: S = S("")
private var nullableTopLevel: S? = S("")

private fun <T> KCallable<T>.callBy(vararg args: Any?): T =
    callBy(parameters.associateWith { args[it.index] })

fun box(): String {
    val c = C()
    assertEquals(Unit, c.nonNullUnboundRef().setter.callBy(c, S("ab")))
    assertEquals(S("ab"), c.nonNullUnboundRef().callBy(c))
    assertEquals(S("ab"), c.nonNullUnboundRef().getter.callBy(c))

    assertEquals(Unit, c.nonNullBoundRef().setter.callBy(S("cd")))
    assertEquals(S("cd"), c.nonNullBoundRef().callBy())
    assertEquals(S("cd"), c.nonNullBoundRef().getter.callBy())

    assertEquals(Unit, c.nullableUnboundRef().setter.callBy(c, S("ab")))
    assertEquals(S("ab"), c.nullableUnboundRef().callBy(c))
    assertEquals(S("ab"), c.nullableUnboundRef().getter.callBy(c))

    assertEquals(Unit, c.nullableBoundRef().setter.callBy(S("cd")))
    assertEquals(S("cd"), c.nullableBoundRef().callBy())
    assertEquals(S("cd"), c.nullableBoundRef().getter.callBy())

    val nonNullTopLevel = ::nonNullTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nonNullTopLevel.setter.callBy(S("ef")))
    assertEquals(S("ef"), nonNullTopLevel.callBy())
    assertEquals(S("ef"), nonNullTopLevel.getter.callBy())

    val nullableTopLevel = ::nullableTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nullableTopLevel.setter.callBy(S("ef")))
    assertEquals(S("ef"), nullableTopLevel.callBy())
    assertEquals(S("ef"), nullableTopLevel.getter.callBy())

    return "OK"
}
