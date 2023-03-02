// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.KMutableProperty2
import kotlin.test.assertEquals

@JvmInline
value class Z(val value1: UInt, val value2: Int) {
    operator fun plus(other: Z): Z = Z(this.value1 + other.value1, this.value2 + other.value2)
}

class C {
    var nonNullMember: Z = Z(0U, 0)
    var nullableMember: Z? = Z(0U, 0)

    private var offset = Z(0U, 0)
    var Z.nonNull_nonNullMemExt: Z
        get() = this + offset
        set(value) { offset = this + value }

    var Z.nonNull_nullableMemExt: Z?
        get() = this + offset
        set(value) { offset = this + value!! }

    var Z?.nullable_nonNullMemExt: Z
        get() = this!! + offset
        set(value) { offset = this!! + value }

    var Z?.nullable_nullableMemExt: Z?
        get() = this!! + offset
        set(value) { offset = this!! + value!! }
}

var nonNullTopLevel: Z = Z(0U, 0)
var nullableTopLevel: Z? = Z(0U, 0)

private var offset = Z(0U, 0)
var Z.nonNull_nonNullExt: Z
    get() = this + offset
    set(value) { offset = this + value }

var Z.nonNull_nullableExt: Z?
    get() = this + offset
    set(value) { offset = this + value!! }

var Z?.nullable_nonNullExt: Z
    get() = this!! + offset
    set(value) { offset = this!! + value }

var Z?.nullable_nullableExt: Z?
    get() = this!! + offset
    set(value) { offset = this!! + value!! }

fun box(): String {
    val one = Z(1U, -1)
    val two = Z(2U, -2)
    val three = Z(3U, -3)

    val c = C()
    assertEquals(Unit, C::nonNullMember.setter.call(c, one))
    assertEquals(one, C::nonNullMember.call(c))
    assertEquals(one, C::nonNullMember.getter.call(c))

    assertEquals(Unit, c::nonNullMember.setter.call(two))
    assertEquals(two, c::nonNullMember.call())
    assertEquals(two, c::nonNullMember.getter.call())

    assertEquals(Unit, C::nullableMember.setter.call(c, one))
    assertEquals(one, C::nullableMember.call(c))
    assertEquals(one, C::nullableMember.getter.call(c))

    assertEquals(Unit, c::nullableMember.setter.call(two))
    assertEquals(two, c::nullableMember.call())
    assertEquals(two, c::nullableMember.getter.call())

    val nonNull_nonNullMemExt = C::class.members.single { it.name == "nonNull_nonNullMemExt" } as KMutableProperty2<C, Z, Z>
    assertEquals(Unit, nonNull_nonNullMemExt.setter.call(c, Z(0U, 0), two))
    assertEquals(three, nonNull_nonNullMemExt.call(c, one))
    assertEquals(three, nonNull_nonNullMemExt.getter.call(c, one))

    val nonNull_nullableMemExt = C::class.members.single { it.name == "nonNull_nullableMemExt" } as KMutableProperty2<C, Z, Z?>
    assertEquals(Unit, nonNull_nullableMemExt.setter.call(c, Z(0U, 0), two))
    assertEquals(three, nonNull_nullableMemExt.call(c, one))
    assertEquals(three, nonNull_nullableMemExt.getter.call(c, one))

    val nullable_nonNullMemExt = C::class.members.single { it.name == "nullable_nonNullMemExt" } as KMutableProperty2<C, Z?, Z>
    assertEquals(Unit, nullable_nonNullMemExt.setter.call(c, Z(0U, 0), two))
    assertEquals(three, nullable_nonNullMemExt.call(c, one))
    assertEquals(three, nullable_nonNullMemExt.getter.call(c, one))

    val nullable_nullableMemExt = C::class.members.single { it.name == "nullable_nullableMemExt" } as KMutableProperty2<C, Z?, Z?>
    assertEquals(Unit, nullable_nullableMemExt.setter.call(c, Z(0U, 0), two))
    assertEquals(three, nullable_nullableMemExt.call(c, one))
    assertEquals(three, nullable_nullableMemExt.getter.call(c, one))

    assertEquals(Unit, ::nonNullTopLevel.setter.call(one))
    assertEquals(one, ::nonNullTopLevel.call())
    assertEquals(one, ::nonNullTopLevel.getter.call())

    assertEquals(Unit, ::nullableTopLevel.setter.call(one))
    assertEquals(one, ::nullableTopLevel.call())
    assertEquals(one, ::nullableTopLevel.getter.call())

    assertEquals(Unit, Z::nonNull_nonNullExt.setter.call(Z(0U, 0), two))
    assertEquals(three, Z::nonNull_nonNullExt.call(one))
    assertEquals(three, Z::nonNull_nonNullExt.getter.call(one))

    assertEquals(Unit, Z::nonNull_nullableExt.setter.call(Z(0U, 0), two))
    assertEquals(three, Z::nonNull_nullableExt.call(one))
    assertEquals(three, Z::nonNull_nullableExt.getter.call(one))

    assertEquals(Unit, Z?::nullable_nonNullExt.setter.call(Z(0U, 0), two))
    assertEquals(three, Z?::nullable_nonNullExt.call(one))
    assertEquals(three, Z?::nullable_nonNullExt.getter.call(one))

    assertEquals(Unit, Z?::nullable_nullableExt.setter.call(Z(0U, 0), two))
    assertEquals(three, Z?::nullable_nullableExt.call(one))
    assertEquals(three, Z?::nullable_nullableExt.getter.call(one))

    return "OK"
}
