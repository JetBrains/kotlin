// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT

import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(a: S, b: S = S("b")): S = a + b
}

fun topLevel(c: S, d: S = S("d")): S = c + d

/* TODO: support constructors with inline class types in the signature (KT-26765)
class D(e: S, f: S = S("f")) {
    val result = e + f
}
*/

fun S.extension(h: S = S("h")): S = this + h

fun box(): String {
    assertEquals(S("ab"), C::member.callBy(C::member.parameters.filter { it.name != "b" }.associate {
        it to (if (it.name == "a") S("a") else C())
    }))

    assertEquals(S("cd"), ::topLevel.callBy(::topLevel.parameters.filter { it.name != "d" }.associate { it to S("c") }))

    // assertEquals(S("ef"), ::D.callBy(::D.parameters.filter { it.name != "f" }.associate { it to S("e") }).result)

    assertEquals(S("gh"), S::extension.callBy(S::extension.parameters.filter { it.name != "h" }.associate { it to S("g") }))


    val boundMember = C()::member
    assertEquals(S("ab"), boundMember.callBy(boundMember.parameters.associate { it to S(it.name!!) }))

    val boundExtension = S("g")::extension
    assertEquals(S("gh"), boundExtension.callBy(boundExtension.parameters.associate { it to S(it.name!!) }))

    return "OK"
}
