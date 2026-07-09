// LANGUAGE: +CompanionBlocksAndExtensions
// ^ On Native `CompanionBlocksAndExtensions` language feature enables the JVM-like initialization.
//   See nativeCompanionInitOrderLegacy for Native behavior without the language feature.
// ISSUE: KT-87516 [K/JS, K/Wasm] Companion objects coming from super interfaces are not initialized
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI

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

fun box(): String {
    l = ""
    A1
    val r1 = l
    if (r1 != "J1.Companion\nI1.Companion\nK1.Companion\nB1.Companion\nL1.Companion\nA1.Companion\n") return "fail test1: '$r1'"

    l = ""
    A2()
    val r2 = l
    if (r2 != "J2.Companion\nI2.Companion\nK2.Companion\nB2.Companion\nL2.Companion\nA2.Companion\nB2.init\nA2.init\n") return "fail test2: '$r2'"

    return "OK"
}
