// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JvmInline
value class S(val value1: UInt, val value2: String) {
    operator fun plus(other: S): S = S(this.value1 * 10U + other.value1, this.value2 + other.value2)
}

class C {
    fun member(a: S, b: S = S(1U, "b2")): S = a + b
}

fun topLevel(c: S, d: S = S(1U, "d2")): S = c + d

class D(e: S, f: S = S(1U, "f2")) {
    val result = e + f
}

fun S.extension(h: S = S(1U, "h2")): S = this + h

fun box(): String {
    assertEquals(S(11U, "a2b2"), C().member(S(1U, "a2")))
    assertEquals(
        S(11U, "a2b2"),
        C::member.callBy(C::member.parameters.filter { it.name != "b" }.associateWith { (if (it.name == "a") S(1U, "a2") else C()) })
    )

    assertEquals(S(11U, "c2d2"), topLevel(S(1U, "c2")))
    assertEquals(S(11U, "c2d2"), ::topLevel.callBy(::topLevel.parameters.filter { it.name != "d" }.associateWith { S(1U, "c2") }))

    assertEquals(S(11U, "e2f2"), ::D.callBy(::D.parameters.filter { it.name != "f" }.associateWith { S(1U, "e2") }).result)

    assertEquals(S(11U, "g2h2"), S(1U, "g2").extension())
    assertEquals(S(11U, "g2h2"), S::extension.callBy(S::extension.parameters.filter { it.name != "h" }.associateWith { S(1U, "g2") }))

    val boundMember = C()::member
    assertEquals(S(11U, "a2b2"), boundMember.callBy(boundMember.parameters.associateWith { S(1U, it.name!! + "2") }))

    val boundExtension = S(1U, "g2")::extension
    assertEquals(S(11U, "g2h2"), boundExtension.callBy(boundExtension.parameters.associateWith { S(1U, it.name!! + "2") }))

    val mfvcConstructor = ::S
    val exception = runCatching { mfvcConstructor.callBy(mapOf(mfvcConstructor.parameters.first() to 1U)) }.exceptionOrNull()!!
    assertTrue(exception is IllegalArgumentException)
    assertTrue(exception.message!!.startsWith("No argument provided for a required parameter: parameter #1 value2 of fun `<init>`(kotlin.UInt, kotlin.String): "), exception.message)
    assertTrue(exception.message!!.endsWith("S"), exception.message)
    
    return "OK"
}
