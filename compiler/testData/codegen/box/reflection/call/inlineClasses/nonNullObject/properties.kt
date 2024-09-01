// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KMutableProperty2
import kotlin.test.assertEquals

@JvmInline
value class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    var nonNullMember: S = S("")
    var nullableMember: S? = S("")

    private var suffix = S("")
    var S.nonNull_nonNullMemExt: S
        get() = this + suffix
        set(value) { suffix = this + value }

    var S.nonNull_nullableMemExt: S?
        get() = this + suffix
        set(value) { suffix = this + value!! }

    var S?.nullable_nonNullMemExt: S
        get() = this!! + suffix
        set(value) { suffix = this!! + value }

    var S?.nullable_nullableMemExt: S?
        get() = this!! + suffix
        set(value) { suffix = this!! + value!! }
}

var nonNullTopLevel: S = S("")
var nullableTopLevel: S? = S("")

private var suffix = S("")
var S.nonNull_nonNullExt: S
    get() = this + suffix
    set(value) { suffix = this + value }

var S.nonNull_nullableExt: S?
    get() = this + suffix
    set(value) { suffix = this + value!! }

var S?.nullable_nonNullExt: S
    get() = this!! + suffix
    set(value) { suffix = this!! + value }

var S?.nullable_nullableExt: S?
    get() = this!! + suffix
    set(value) { suffix = this!! + value!! }

fun box(): String {
    val c = C()
    assertEquals(Unit, C::nonNullMember.setter.call(c, S("ab")))
    assertEquals(S("ab"), C::nonNullMember.call(c))
    assertEquals(S("ab"), C::nonNullMember.getter.call(c))

    assertEquals(Unit, c::nonNullMember.setter.call(S("cd")))
    assertEquals(S("cd"), c::nonNullMember.call())
    assertEquals(S("cd"), c::nonNullMember.getter.call())

    assertEquals(Unit, C::nullableMember.setter.call(c, S("ab")))
    assertEquals(S("ab"), C::nullableMember.call(c))
    assertEquals(S("ab"), C::nullableMember.getter.call(c))

    assertEquals(Unit, c::nullableMember.setter.call(S("cd")))
    assertEquals(S("cd"), c::nullableMember.call())
    assertEquals(S("cd"), c::nullableMember.getter.call())

    val nonNull_nonNullMemExt = C::class.members.single { it.name == "nonNull_nonNullMemExt" } as KMutableProperty2<C, S, S>
    assertEquals(Unit, nonNull_nonNullMemExt.setter.call(c, S(""), S("f")))
    assertEquals(S("ef"), nonNull_nonNullMemExt.call(c, S("e")))
    assertEquals(S("ef"), nonNull_nonNullMemExt.getter.call(c, S("e")))

    val nonNull_nullableMemExt = C::class.members.single { it.name == "nonNull_nullableMemExt" } as KMutableProperty2<C, S, S?>
    assertEquals(Unit, nonNull_nullableMemExt.setter.call(c, S(""), S("f")))
    assertEquals(S("ef"), nonNull_nullableMemExt.call(c, S("e")))
    assertEquals(S("ef"), nonNull_nullableMemExt.getter.call(c, S("e")))

    val nullable_nonNullMemExt = C::class.members.single { it.name == "nullable_nonNullMemExt" } as KMutableProperty2<C, S?, S>
    assertEquals(Unit, nullable_nonNullMemExt.setter.call(c, S(""), S("f")))
    assertEquals(S("ef"), nullable_nonNullMemExt.call(c, S("e")))
    assertEquals(S("ef"), nullable_nonNullMemExt.getter.call(c, S("e")))

    val nullable_nullableMemExt = C::class.members.single { it.name == "nullable_nullableMemExt" } as KMutableProperty2<C, S?, S?>
    assertEquals(Unit, nullable_nullableMemExt.setter.call(c, S(""), S("f")))
    assertEquals(S("ef"), nullable_nullableMemExt.call(c, S("e")))
    assertEquals(S("ef"), nullable_nullableMemExt.getter.call(c, S("e")))

    assertEquals(Unit, ::nonNullTopLevel.setter.call(S("gh")))
    assertEquals(S("gh"), ::nonNullTopLevel.call())
    assertEquals(S("gh"), ::nonNullTopLevel.getter.call())

    assertEquals(Unit, ::nullableTopLevel.setter.call(S("gh")))
    assertEquals(S("gh"), ::nullableTopLevel.call())
    assertEquals(S("gh"), ::nullableTopLevel.getter.call())

    assertEquals(Unit, S::nonNull_nonNullExt.setter.call(S(""), S("j")))
    assertEquals(S("ij"), S::nonNull_nonNullExt.call(S("i")))
    assertEquals(S("ij"), S::nonNull_nonNullExt.getter.call(S("i")))

    assertEquals(Unit, S::nonNull_nullableExt.setter.call(S(""), S("j")))
    assertEquals(S("ij"), S::nonNull_nullableExt.call(S("i")))
    assertEquals(S("ij"), S::nonNull_nullableExt.getter.call(S("i")))

    assertEquals(Unit, S?::nullable_nonNullExt.setter.call(S(""), S("j")))
    assertEquals(S("ij"), S?::nullable_nonNullExt.call(S("i")))
    assertEquals(S("ij"), S?::nullable_nonNullExt.getter.call(S("i")))

    assertEquals(Unit, S?::nullable_nullableExt.setter.call(S(""), S("j")))
    assertEquals(S("ij"), S?::nullable_nullableExt.call(S("i")))
    assertEquals(S("ij"), S?::nullable_nullableExt.getter.call(S("i")))

    return "OK"
}
