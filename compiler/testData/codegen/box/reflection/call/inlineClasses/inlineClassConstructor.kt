// IGNORE_BACKEND: JS_IR, JS, NATIVE, JVM_IR
// WITH_REFLECT
import kotlin.reflect.KCallable
import kotlin.test.assertEquals

inline class Z(val x: Int) {
    constructor(a: Int, b: Int) : this(a + b)
}

inline class L(val x: Long) {
    constructor(a: Long, b: Long) : this(a + b)
}

inline class S(val x: String) {
    constructor(a: String, b: String) : this(a + b)
}

inline class A(val x: Any) {
    constructor(a: String, b: String) : this(a + b)
}

inline class Z2(val z: Z)

fun box(): String {
    val ctorZ1: (Int) -> Z = ::Z
    val ctorZ2: (Int, Int) -> Z = ::Z
    val ctorL1: (Long) -> L = ::L
    val ctorL2: (Long, Long) -> L = ::L
    val ctorS1: (String) -> S = ::S
    val ctorS2: (String, String) -> S = ::S
    val ctorA1: (Any) -> A = ::A
    val ctorA2: (String, String) -> A = ::A

    assertEquals(Z(42), (ctorZ1 as KCallable<Z>).call(42))
    assertEquals(Z(123), (ctorZ2 as KCallable<Z>).call(100, 23))
    assertEquals(L(1L), (ctorL1 as KCallable<L>).call(1L))
    assertEquals(L(123L), (ctorL2 as KCallable<L>).call(100L, 23L))
    assertEquals(S("abc"), (ctorS1 as KCallable<S>).call("abc"))
    assertEquals(S("abc"), (ctorS2 as KCallable<S>).call("ab", "c"))
    assertEquals(A("abc"), (ctorA1 as KCallable<A>).call("abc"))
    assertEquals(A("abc"), (ctorA2 as KCallable<A>).call("a", "bc"))

    assertEquals(Z2(Z(42)), ::Z2.call(Z(42)))

    return "OK"
}