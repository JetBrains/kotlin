// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

@JvmInline
value class S(val value: String?) {
    operator fun plus(other: S): S = S(this.value!! + other.value!!)
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

fun box(): String {
    val c = C()
    assertEquals(Unit, c.nonNullUnboundRef().setter.call(c, S("ab")))
    assertEquals(S("ab"), c.nonNullUnboundRef().call(c))
    assertEquals(S("ab"), c.nonNullUnboundRef().getter.call(c))

    assertEquals(Unit, c.nonNullBoundRef().setter.call(S("cd")))
    assertEquals(S("cd"), c.nonNullBoundRef().call())
    assertEquals(S("cd"), c.nonNullBoundRef().getter.call())

    assertEquals(Unit, c.nullableUnboundRef().setter.call(c, S("ab")))
    assertEquals(S("ab"), c.nullableUnboundRef().call(c))
    assertEquals(S("ab"), c.nullableUnboundRef().getter.call(c))

    assertEquals(Unit, c.nullableBoundRef().setter.call(S("cd")))
    assertEquals(S("cd"), c.nullableBoundRef().call())
    assertEquals(S("cd"), c.nullableBoundRef().getter.call())

    val nonNullTopLevel = ::nonNullTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nonNullTopLevel.setter.call(S("ef")))
    assertEquals(S("ef"), nonNullTopLevel.call())
    assertEquals(S("ef"), nonNullTopLevel.getter.call())

    val nullableTopLevel = ::nullableTopLevel.apply { isAccessible = true }
    assertEquals(Unit, nullableTopLevel.setter.call(S("ef")))
    assertEquals(S("ef"), nullableTopLevel.call())
    assertEquals(S("ef"), nullableTopLevel.getter.call())

    return "OK"
}
