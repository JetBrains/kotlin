// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.test.assertEquals

@JvmInline
value class SInt(val value1: Int, val value2: Int?) {
    operator fun plus(other: SInt): SInt = SInt(this.value1 + other.value1, this.value2!! + other.value2!!)
    
}

@JvmInline
value class SString(val value1: String, val value2: String?) {
    operator fun plus(other: SString): SString = SString(this.value1 + other.value1, this.value2!! + other.value2!!)
}

@JvmInline
value class S(val value1: SInt, val value2: SString, val value3: SInt?, val value4: SString?) {
    operator fun plus(other: S): S =
        S(this.value1 + other.value1, this.value2 + other.value2, this.value3!! + other.value3!!, this.value4!! + other.value4!!)
}

fun s(x: Int) = S(
    SInt(1 * x, 10 * x),
    SString(('a' + 4 * x).toString(), ('b' + 4 * x).toString()),
    SInt(100 * x, 1000 * x),
    SString(('c' + 4 * x).toString(), ('d' + 4 * x).toString()),
)

fun s(x: Int, y: Int) = s(x) + s(y)

class C {
    fun member(a: S, b: S = s(2)): S = a + b
}

fun topLevel(c: S, d: S = s(4)): S = c + d

class D(e: S, f: S = s(6)) {
    val result = e + f
}

fun S.extension(h: S = s(8)): S = this + h

fun box(): String {
    assertEquals(s(1, 2), C().member(s(1)))
    assertEquals(
        s(1, 2),
        C::member.callBy(C::member.parameters.filter { it.name != "b" }.associateWith { (if (it.name == "a") s(1) else C()) })
    )

    assertEquals(s(3, 4), topLevel(s(3)))
    assertEquals(s(3, 4), ::topLevel.callBy(::topLevel.parameters.filter { it.name != "d" }.associateWith { s(3) }))

    assertEquals(s(5, 6), ::D.callBy(::D.parameters.filter { it.name != "f" }.associateWith { s(5) }).result)

    assertEquals(s(7, 8), s(7).extension())
    assertEquals(s(7, 8), S::extension.callBy(S::extension.parameters.filter { it.name != "h" }.associateWith { s(7) }))

    val boundMember = C()::member
    assertEquals(s(1, 2), boundMember.callBy(boundMember.parameters.associateWith { s(it.name!![0] - 'a' + 1) }))

    val boundExtension = s(7)::extension
    assertEquals(s(7, 8), boundExtension.callBy(boundExtension.parameters.associateWith { s(it.name!![0] - 'a' + 1) }))

    return "OK"
}
