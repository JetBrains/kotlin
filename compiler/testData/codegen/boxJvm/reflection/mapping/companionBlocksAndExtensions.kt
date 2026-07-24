// TARGET_BACKEND: JVM
// WITH_REFLECT
// LANGUAGE: +CompanionBlocks +CompanionExtensions

import kotlin.reflect.jvm.*
import kotlin.test.assertEquals

class A {
    companion {
        fun f(x: Int = 1): Int = x + 1
        var p: String = "a"
    }
}

companion fun A.g(x: Int = 1): Int = x + 10
companion var A.q: String
    get() = "b"
    set(value) {}

fun box(): String {
    val fj = A::f.javaMethod ?: return "Fail: no javaMethod for A::f"
    assertEquals(A::f, fj.kotlinFunction)

    val pj = A::p.javaField ?: return "Fail: no javaField for A::p"
    assertEquals(A::p, pj.kotlinProperty)
    assertEquals("getP", A::p.javaGetter?.name)
    assertEquals("setP", A::p.javaSetter?.name)

    val gj = A::g.javaMethod ?: return "Fail: no javaMethod for A::g"
    assertEquals(A::g, gj.kotlinFunction)

    assertEquals("getQ", A::q.javaGetter?.name)
    assertEquals("setQ", A::q.javaSetter?.name)

    return "OK"
}
