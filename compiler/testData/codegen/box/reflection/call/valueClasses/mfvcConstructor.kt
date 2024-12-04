// TARGET_BACKEND: JVM_IR
// WITH_REFLECT
// LANGUAGE: +ValueClasses

import kotlin.reflect.KCallable
import kotlin.test.assertEquals

@JvmInline
value class Z(val x1: UInt, val x2: Int) {
    constructor(a: UInt, b: UInt, c: Int, d: Int) : this(a + b, c + d)
}

@JvmInline
value class L(val x1: ULong, val x2: Long) {
    constructor(a: ULong, b: ULong, c: Long, d: Long) : this(a + b, c + d)
}

@JvmInline
value class S1(val x1: String, val x2: String) {
    constructor(a: String, b: String, c: String, d: String) : this(a + b, c + d)
}

@JvmInline
value class S2(val x1: String?, val x2: String?) {
    constructor(a: String?, b: String?, c: String?, d: String?) : this(a!! + b!!, c!! + d!!)
}

@JvmInline
value class A(val x1: Any, val x2: Any) {
    constructor(a: String, b: String, c: String, d: String) : this(a + b, c + d)
}

@JvmInline
value class Z2(val z1: Z, val z2: Z) {
    constructor(z1: Z, z2: Z, z3: Z, z4: Z) : this(Z(z1.x1 + z2.x1, z1.x2 + z2.x2), Z(z3.x1 + z4.x1, z3.x2 + z4.x2))
}

@JvmInline
value class Z3(val z1: Z?, val z2: Z?) {
    constructor(z1: Z?, z2: Z?, z3: Z?, z4: Z?) : this(Z(z1!!.x1 + z2!!.x1, z1!!.x2 + z2!!.x2), Z(z3!!.x1 + z4!!.x1, z3!!.x2 + z4!!.x2))
}

fun box(): String {
    val ctorZ1_1: (UInt, Int) -> Z = ::Z
    val ctorZ1_2: (UInt, UInt, Int, Int) -> Z = ::Z
    val ctorL1: (ULong, Long) -> L = ::L
    val ctorL2: (ULong, ULong, Long, Long) -> L = ::L
    val ctorS1_1: (String, String) -> S1 = ::S1
    val ctorS1_2: (String, String, String, String) -> S1 = ::S1
    val ctorS2_1: (String, String) -> S2 = ::S2
    val ctorS2_2: (String, String, String, String) -> S2 = ::S2
    val ctorA1: (Any, Any) -> A = ::A
    val ctorA2: (String, String, String, String) -> A = ::A
    val ctorZ2_2: (Z, Z) -> Z2 = ::Z2
    val ctorZ2_4: (Z, Z, Z, Z) -> Z2 = ::Z2
    val ctorZ3_2: (Z, Z) -> Z3 = ::Z3
    val ctorZ3_4: (Z, Z, Z, Z) -> Z3 = ::Z3

    assertEquals(Z(42U, 43), (ctorZ1_1 as KCallable<Z>).call(42U, 43))
    assertEquals(Z(123U, 224), (ctorZ1_2 as KCallable<Z>).call(100U, 23U, 200, 24))
    assertEquals(L(1UL, 2L), (ctorL1 as KCallable<L>).call(1UL, 2L))
    assertEquals(L(123UL, 224L), (ctorL2 as KCallable<L>).call(100UL, 23UL, 200L, 24L))
    assertEquals(S1("abc", "def"), (ctorS1_1 as KCallable<S1>).call("abc", "def"))
    assertEquals(S1("abc", "def"), (ctorS1_2 as KCallable<S1>).call("ab", "c", "de", "f"))
    assertEquals(S2("abc", "def"), (ctorS2_1 as KCallable<S2>).call("abc", "def"))
    assertEquals(S2("abc", "def"), (ctorS2_2 as KCallable<S2>).call("ab", "c", "de", "f"))
    assertEquals(A("abc", "def"), (ctorA1 as KCallable<A>).call("abc", "def"))
    assertEquals(A("abc", "def"), (ctorA2 as KCallable<A>).call("a", "bc", "d", "ef"))

    assertEquals(Z2(Z(42U, 43), Z(44U, 45)), (ctorZ2_2 as KCallable<Z2>).call(Z(42U, 43), Z(44U, 45)))
    assertEquals(Z3(Z(42U, 43), Z(44U, 45)), (ctorZ3_2 as KCallable<Z3>).call(Z(42U, 43), Z(44U, 45)))
    assertEquals(
        Z2(Z(142U, 243), Z(344U, 445)),
        (ctorZ2_4 as KCallable<Z2>).call(Z(42U, 43), Z(100U, 200), Z(44U, 45), Z(300U, 400))
    )
    assertEquals(
        Z3(Z(142U, 243), Z(344U, 445)),
        (ctorZ3_4 as KCallable<Z3>).call(Z(42U, 43), Z(100U, 200), Z(44U, 45), Z(300U, 400))
    )

    return "OK"
}
