// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

inline class S(val value: String) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(a: S, b: S = S("b")): S = a + b
}

fun topLevel(c: S, d: S = S("d")): S = c + d

class D(e: S, f: S = S("f")) {
    val result = e + f
}

fun S.extension(h: S = S("h")): S = this + h

fun box(): String {
    assertEquals(S("ab"), C().member(S("a")))
    assertEquals(
        S("ab"),
        C::member.callBy(C::member.parameters.filter { it.name != "b" }.associateWith { (if (it.name == "a") S("a") else C()) })
    )

    assertEquals(S("cd"), topLevel(S("c")))
    assertEquals(S("cd"), ::topLevel.callBy(::topLevel.parameters.filter { it.name != "d" }.associateWith { S("c") }))

    assertEquals(S("ef"), ::D.callBy(::D.parameters.filter { it.name != "f" }.associateWith { S("e") }).result)

    assertEquals(S("gh"), S("g").extension())
    assertEquals(S("gh"), S::extension.callBy(S::extension.parameters.filter { it.name != "h" }.associateWith { S("g") }))

    val boundMember = C()::member
    assertEquals(S("ab"), boundMember.callBy(boundMember.parameters.associateWith { S(it.name!!) }))

    val boundExtension = S("g")::extension
    assertEquals(S("gh"), boundExtension.callBy(boundExtension.parameters.associateWith { S(it.name!!) }))

    return "OK"
}
