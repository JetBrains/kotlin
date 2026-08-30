// LANGUAGE: +CompanionBlocks
// ^ On Native `CompanionBlocks` language feature enables the JVM-like initialization.
//   See nativeCompanionInitOrderLegacy for Native behavior without the language feature.

var l = ""
private fun log(t: String) {
    l += t + "\n"
}

// Each test uses its own class hierarchy so companions are initialized fresh.

// multiple interface inheritance
interface I1 {
    fun i() {}
    companion object { init { log("I1.Companion") } }
}
interface J1 {
    fun j() {}
    companion object { init { log("J1.Companion") } }
}
interface K1 : I1 {
    fun k() {}
    companion object { init { log("K1.Companion") } }
}
interface L1 : J1 {
    fun l() {}
    companion object { init { log("L1.Companion") } }
}
interface M1 {
    companion object { init { log("M1.Companion") } }
}
open class B1 : J1, K1 {
    companion object { init { log("B1.Companion") } }
}
class A1: B1(), L1, M1 {
    companion object { init { log("A1.Companion") } }
}

// multiple interface inheritance; with instance creation
interface I2 {
    fun i() {}
    companion object { init { log("I2.Companion") } }
}
interface J2 {
    fun j() {}
    companion object { init { log("J2.Companion") } }
}
interface K2 : I2 {
    fun k() {}
    companion object { init { log("K2.Companion") } }
}
interface L2 : J2 {
    fun l() {}
    companion object { init { log("L2.Companion") } }
}
interface M2 {
    companion object { init { log("M2.Companion") } }
}
open class B2 : J2, K2 {
    init { log("B2.init") }
    companion object { init { log("B2.Companion") } }
}
class A2: B2(), L2, M2 {
    init { log("A2.init") }
    companion object { init { log("A2.Companion") } }
}

// interface without non-abstract members extending one with non-abstract members
// The intermediate interface J3 should NOT be initialized when A3 is accessed, because it
// has no non-abstract members. Only I3 (which declares foo()) should be initialized.
interface I3 {
    fun foo() {}
    companion object { init { log("I3.Companion") } }
}
interface J3 : I3 {
    companion object { init { log("J3.Companion") } }
}
class A3 : J3 {
    companion object { init { log("A3.Companion") } }
}

// same as test3 but with explicit access to J3 after A3
interface I4 {
    fun foo() {}
    companion object { init { log("I4.Companion") } }
}
interface J4 : I4 {
    companion object { init { log("J4.Companion") } }
}
class A4 : J4 {
    companion object { init { log("A4.Companion") } }
}

// implementing the same interface multiple times through the interface chain
interface A5 {
    fun a() {}
    companion object { init { log("A5.Companion") } }
}
interface B5 : A5 {
    fun b() {}
    companion object { init { log("B5.Companion") } }
}
interface C5 : B5, A5 {
    fun c() {}
    companion object { init { log("C5.Companion") } }
}
class D5 : C5, A5 {
    companion object { init { log("D5.Companion") } }
}

fun box(): String {
    l = ""
    A1
    val r1 = l
    if (r1 != "J1.Companion\nI1.Companion\nK1.Companion\nB1.Companion\nL1.Companion\nA1.Companion\n") return "fail test1: '$r1'"

    l = ""
    A2()
    val r2 = l
    if (r2 != "J2.Companion\nI2.Companion\nK2.Companion\nB2.Companion\nL2.Companion\nA2.Companion\nB2.init\nA2.init\n") return "fail test2: '$r2'"

    // J3 should NOT be initialized here, only I3 (declares foo()) and A3
    l = ""
    A3
    val r3 = l
    if (r3 != "I3.Companion\nA3.Companion\n") return "fail test3: '$r3'"

    // J4 should be initialized only on direct access, not during A4 initialization
    l = ""
    A4
    log("--")
    J4
    val r4 = l
    if (r4 != "I4.Companion\nA4.Companion\n--\nJ4.Companion\n") return "fail test4: '$r4'"

    // A5 should be initialized only once, despite being inherited by B5, C5, and D5
    l = ""
    D5
    val r5 = l
    if (r5 != "A5.Companion\nB5.Companion\nC5.Companion\nD5.Companion\n") return "fail test5: '$r5'"

    return "OK"
}
