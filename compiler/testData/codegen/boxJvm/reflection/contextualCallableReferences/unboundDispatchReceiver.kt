// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +ContextParameters +CallableReferencesToContextual

import kotlin.test.assertEquals

@JvmInline
value class Z(val value: String)

class A(val a: String) {
    context(z: Z)
    fun ctxValue(y: String): String = a + z.value + y

    context(z: Z)
    fun ctxAndValue(y: Z): String = a + z.value + y.value

    context(c1: String, c2: Z)
    fun twoCtx(y: Z): String = a + c1 + c2.value + y.value

    context(c: String)
    fun withDefault(y: Z = Z("d")): Z = Z(a + c + y.value)
}

@JvmInline
value class W(val w: String) {
    context(c: String)
    fun member(y: Z): Z = Z(w + c + y.value)

    context(c: String)
    fun withDefault(y: Z = Z("d")): Z = Z(w + c + y.value)
}

fun box(): String {
    context(Z("q")) {
        assertEquals("aqy", (A::ctxValue).call(A("a"), "y"))
        assertEquals("aqy", (A("a")::ctxValue).call("y"))
        assertEquals("aqy", (A::ctxAndValue).call(A("a"), Z("y")))
        assertEquals("aqy", (A("a")::ctxAndValue).call(Z("y")))
        context("X") {
            assertEquals("aXqy", (A::twoCtx).call(A("a"), Z("y")))
        }
    }
    context("X") {
        assertEquals(Z("wXy"), (W::member).call(W("w"), Z("y")))
        assertEquals(Z("wXy"), (W("w")::member).call(Z("y")))

        val bound = A("a")::withDefault
        assertEquals(Z("aXd"), bound.callBy(emptyMap()))
        assertEquals(Z("aXy"), bound.callBy(mapOf(bound.parameters.single() to Z("y"))))
        val unbound = A::withDefault
        assertEquals(Z("aXd"), unbound.callBy(mapOf(unbound.parameters[0] to A("a"))))
        assertEquals(Z("aXy"), unbound.callBy(mapOf(unbound.parameters[0] to A("a"), unbound.parameters[1] to Z("y"))))

        assertEquals(Z("wXd"), (W("w")::withDefault).callBy(emptyMap()))
        val unboundW = W::withDefault
        assertEquals(Z("wXd"), unboundW.callBy(mapOf(unboundW.parameters[0] to W("w"))))
        assertEquals(Z("wXy"), unboundW.callBy(mapOf(unboundW.parameters[0] to W("w"), unboundW.parameters[1] to Z("y"))))
    }
    return "OK"
}
