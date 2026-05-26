// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6
// DONT_TARGET_EXACT_BACKEND: NATIVE
// See KT-84267 K/Wasm: init order of companion objects is different from JVM

var l = ""
private fun log(t: String) {
    l += t + "\n"
}

// Each test uses its own class hierarchy so companions are initialized fresh.

// companion-only access (no instance created)
open class B1 {
    init { log("B1.init#1") }
    companion object { init { log("B1.Companion") } }
    init { log("B1.init#2") }
}
class A1 : B1() {
    init { log("A1.init#1") }
    companion object { init { log("A1.Companion") } }
    init { log("A1.init#2") }
}

// instance creation (triggers both companions then instance inits)
open class B2 {
    init { log("B2.init#1") }
    companion object { init { log("B2.Companion") } }
    init { log("B2.init#2") }
}
class A2 : B2() {
    init { log("A2.init#1") }
    companion object { init { log("A2.Companion") } }
    init { log("A2.init#2") }
}

// companion access then instance creation
open class B3 {
    init { log("B3.init#1") }
    companion object { init { log("B3.Companion") } }
    init { log("B3.init#2") }
}
class A3 : B3() {
    init { log("A3.init#1") }
    companion object { init { log("A3.Companion") } }
    init { log("A3.init#2") }
}

// instance creation then companion access
open class B4 {
    init { log("B4.init#1") }
    companion object { init { log("B4.Companion") } }
    init { log("B4.init#2") }
}
class A4 : B4() {
    init { log("A4.init#1") }
    companion object { init { log("A4.Companion") } }
    init { log("A4.init#2") }
}

// 3-level hierarchy with companion access only
open class C5 {
    companion object { init { log("C5.Companion") } }
}
open class B5 : C5() {
    companion object { init { log("B5.Companion") } }
}
class A5 : B5() {
    companion object { init { log("A5.Companion") } }
}

// 3-level hierarchy with instance creation
open class C6 {
    init { log("C6.init") }
    companion object { init { log("C6.Companion") } }
}
open class B6 : C6() {
    init { log("B6.init") }
    companion object { init { log("B6.Companion") } }
}
class A6 : B6() {
    init { log("A6.init") }
    companion object { init { log("A6.Companion") } }
}

// intermediate class with no companion, companion access only otherwise.
open class C7 {
    companion object { init { log("C7.Companion") } }
}
open class B7 : C7()  // no companion
class A7 : B7() {
    companion object { init { log("A7.Companion") } }
}

// intermediate class with no companion; instance creation
open class C8 {
    init { log("C8.init") }
    companion object { init { log("C8.Companion") } }
}
open class B8 : C8()  // no companion
class A8 : B8() {
    init { log("A8.init") }
    companion object { init { log("A8.Companion") } }
}

fun box(): String {
    l = ""
    A1
    val r1 = l
    if (r1 != "B1.Companion\nA1.Companion\n") return "fail test1: '$r1'"

    l = ""
    A2()
    val r2 = l
    if (r2 != "B2.Companion\nA2.Companion\nB2.init#1\nB2.init#2\nA2.init#1\nA2.init#2\n") return "fail test2: '$r2'"

    l = ""
    A3
    log("--")
    A3()
    val r3 = l
    if (r3 != "B3.Companion\nA3.Companion\n--\nB3.init#1\nB3.init#2\nA3.init#1\nA3.init#2\n") return "fail test3: '$r3'"

    l = ""
    A4()
    log("--")
    A4
    val r4 = l
    if (r4 != "B4.Companion\nA4.Companion\nB4.init#1\nB4.init#2\nA4.init#1\nA4.init#2\n--\n") return "fail test4: '$r4'"

    l = ""
    A5
    val r5 = l
    if (r5 != "C5.Companion\nB5.Companion\nA5.Companion\n") return "fail test5: '$r5'"

    l = ""
    A6()
    val r6 = l
    if (r6 != "C6.Companion\nB6.Companion\nA6.Companion\nC6.init\nB6.init\nA6.init\n") return "fail test6: '$r6'"

    l = ""
    A7
    val r7 = l
    if (r7 != "C7.Companion\nA7.Companion\n") return "fail test7: '$r7'"

    l = ""
    A8()
    val r8 = l
    if (r8 != "C8.Companion\nA8.Companion\nC8.init\nA8.init\n") return "fail test8: '$r8'"

    return "OK"
}
