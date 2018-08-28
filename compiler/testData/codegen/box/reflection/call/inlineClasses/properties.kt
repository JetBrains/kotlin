// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT

import kotlin.reflect.KMutableProperty2
import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    var member: S = S("")

    private var suffix = S("")
    var S.memExt: S
        get() = this + suffix
        set(value) { suffix = this + value }
}

var topLevel: S = S("")

private var suffix = S("")
var S.ext: S
    get() = this + suffix
    set(value) { suffix = this + value }

fun box(): String {
    val c = C()
    assertEquals(Unit, C::member.setter.call(c, S("ab")))
    assertEquals(S("ab"), C::member.call(c))
    assertEquals(S("ab"), C::member.getter.call(c))

    assertEquals(Unit, c::member.setter.call(S("cd")))
    assertEquals(S("cd"), c::member.call())
    assertEquals(S("cd"), c::member.getter.call())

    val memExt = C::class.members.single { it.name == "memExt" } as KMutableProperty2<C, S, S>
    assertEquals(Unit, memExt.setter.call(c, S(""), S("f")))
    assertEquals(S("ef"), memExt.call(c, S("e")))
    assertEquals(S("ef"), memExt.getter.call(c, S("e")))

    assertEquals(Unit, ::topLevel.setter.call(S("gh")))
    assertEquals(S("gh"), ::topLevel.call())
    assertEquals(S("gh"), ::topLevel.getter.call())

    assertEquals(Unit, S::ext.setter.call(S(""), S("j")))
    assertEquals(S("ij"), S::ext.call(S("i")))
    assertEquals(S("ij"), S::ext.getter.call(S("i")))

    return "OK"
}
