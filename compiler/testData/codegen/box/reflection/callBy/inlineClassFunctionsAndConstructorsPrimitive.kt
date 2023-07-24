// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.test.assertEquals

inline class S(val value: Int) {
    operator fun plus(other: S): S = S(this.value + other.value)
}

class C {
    fun member(a: S, b: S = S(2)): S = a + b
}

fun topLevel(c: S, d: S = S(4)): S = c + d

class D(e: S, f: S = S(6)) {
    val result = e + f
}

fun S.extension(h: S = S(8)): S = this + h

fun box(): String {
    assertEquals(S(3), C().member(S(1)))
    assertEquals(
        S(3),
        C::member.callBy(C::member.parameters.filter { it.name != "b" }.associateWith { (if (it.name == "a") S(1) else C()) })
    )

    assertEquals(S(7), topLevel(S(3)))
    assertEquals(S(7), ::topLevel.callBy(::topLevel.parameters.filter { it.name != "d" }.associateWith { S(3) }))

    assertEquals(S(11), ::D.callBy(::D.parameters.filter { it.name != "f" }.associateWith { S(5) }).result)

    assertEquals(S(15), S(7).extension())
    assertEquals(S(15), S::extension.callBy(S::extension.parameters.filter { it.name != "h" }.associateWith { S(7) }))

    val boundMember = C()::member
    assertEquals(S(3), boundMember.callBy(boundMember.parameters.associateWith { S(it.name!![0] - 'a' + 1) }))

    val boundExtension = S(7)::extension
    assertEquals(S(15), boundExtension.callBy(boundExtension.parameters.associateWith { S(it.name!![0] - 'a' + 1) }))

    return "OK"
}
