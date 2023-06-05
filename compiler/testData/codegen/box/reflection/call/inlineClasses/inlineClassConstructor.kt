// TARGET_BACKEND: JVM
// WITH_REFLECT

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

inline class Z(val x: Int) {
    constructor(a: Int, b: Int) : this(a + b)
}

inline class L(val x: Long) {
    constructor(a: Long, b: Long) : this(a + b)
}

inline class S1(val x: String) {
    constructor(a: String, b: String) : this(a + b)
}

inline class S2(val x: String?) {
    constructor(a: String?, b: String?) : this(a!! + b!!)
}

inline class A(val x: Any) {
    constructor(a: String, b: String) : this(a + b)
}

inline class Z2(val z: Z)
inline class Z3(val z: Z?)

fun box(): String {
    val ctorZ1_1: (Int) -> Z = ::Z
    val ctorZ1_2: (Int, Int) -> Z = ::Z
    val ctorL1: (Long) -> L = ::L
    val ctorL2: (Long, Long) -> L = ::L
    val ctorS1_1: (String) -> S1 = ::S1
    val ctorS1_2: (String, String) -> S1 = ::S1
    val ctorS2_1: (String) -> S2 = ::S2
    val ctorS2_2: (String, String) -> S2 = ::S2
    val ctorA1: (Any) -> A = ::A
    val ctorA2: (String, String) -> A = ::A

    assertEquals(Z(42), (ctorZ1_1 as KCallable<Z>).call(42))
    assertEquals(Z(123), (ctorZ1_2 as KCallable<Z>).call(100, 23))
    assertEquals(L(1L), (ctorL1 as KCallable<L>).call(1L))
    assertEquals(L(123L), (ctorL2 as KCallable<L>).call(100L, 23L))
    assertEquals(S1("abc"), (ctorS1_1 as KCallable<S1>).call("abc"))
    assertEquals(S1("abc"), (ctorS1_2 as KCallable<S1>).call("ab", "c"))
    assertEquals(S2("abc"), (ctorS2_1 as KCallable<S2>).call("abc"))
    assertEquals(S2("abc"), (ctorS2_2 as KCallable<S2>).call("ab", "c"))
    assertEquals(A("abc"), (ctorA1 as KCallable<A>).call("abc"))
    assertEquals(A("abc"), (ctorA2 as KCallable<A>).call("a", "bc"))

    assertEquals(Z2(Z(42)), ::Z2.call(Z(42)))
    assertEquals(Z3(Z(42)), ::Z3.call(Z(42)))

    return "OK"
}
