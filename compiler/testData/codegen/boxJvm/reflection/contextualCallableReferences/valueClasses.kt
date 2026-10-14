// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters +CallableReferencesToContextual

import kotlin.test.assertEquals

@JvmInline
value class Z(val value: String)

context(c: String)
fun topLevelValueParam(z: Z): Z = Z(c + z.value)

context(c: String)
fun Z.valueExtension(y: Z): Z = Z(c + value + y.value)

context(c: String)
fun onlyContext(): Z = Z(c)

class A(val a: String) {
    context(c: String)
    fun member(y: Z): Z = Z(a + c + y.value)
}

context(z: Z)
fun valueContext(y: Z): Z = Z(z.value + y.value)

@JvmInline
value class V(val v: String) {
    context(c: String)
    fun member(y: Z): Z = Z(v + c + y.value)

    context(c: String)
    val prop: String get() = v + c

    context(z: Z)
    fun valueContextMember(y: Z): Z = Z(v + z.value + y.value)
}

fun box(): String {
    context("X") {
        val f1 = ::topLevelValueParam
        assertEquals(Z("Xz"), f1.call(Z("z")))

        val f2 = Z("r")::valueExtension
        assertEquals(Z("Xry"), f2.call(Z("y")))

        val f3 = ::onlyContext
        assertEquals(Z("X"), f3.call())

        val f4 = A("a")::member
        assertEquals(Z("aXy"), f4.call(Z("y")))

        val f5 = A::member
        assertEquals(Z("aXy"), f5.call(A("a"), Z("y")))

        val m1 = V("v")::member
        assertEquals(Z("vXy"), m1.call(Z("y")))

        val m2 = V::member
        assertEquals(Z("vXy"), m2.call(V("v"), Z("y")))

        val p1 = V("v")::prop
        assertEquals("vX", p1.call())
        assertEquals("vX", p1.getter.call())

        val p2 = V::prop
        assertEquals("vX", p2.call(V("v")))
        assertEquals("vX", p2.getter.call(V("v")))
    }

    context(Z("q")) {
        val f6 = ::valueContext
        assertEquals(Z("qy"), f6.call(Z("y")))

        val m3 = V("v")::valueContextMember
        assertEquals(Z("vqy"), m3.call(Z("y")))

        val m4 = V::valueContextMember
        assertEquals(Z("vqy"), m4.call(V("v"), Z("y")))
    }
    return "OK"
}
