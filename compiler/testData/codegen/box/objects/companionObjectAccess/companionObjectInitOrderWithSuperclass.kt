// LANGUAGE: +CompanionBlocksAndExtensions
// ^ On Native `CompanionBlocksAndExtensions` language feature enables the JVM-like initialization.
//   See nativeCompanionInitOrderLegacy for Native behavior without the language feature.
// ISSUE: KT-84267 K/Wasm: init order of companion objects is different from JVM
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS,Wasm-js:2.3,2.4
// ^^^KT-84267 is fixed in 2.4.20-beta1 for WASM
// ^^^KT-40768 is fixed in 2.4.20-beta1 for JS

var l = ""
private fun log(t: String) {
    l += t + "\n"
}

private fun logValue(t: String): String {
    log(t)
    return t
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

// parent companion access and repeated child companion access
open class B9 {
    companion object { init { log("B9.Companion") } }
}
class A9 : B9() {
    companion object { init { log("A9.Companion") } }
}

// named private parent companion
open class B10 {
    private companion object Parent { init { log("B10.Parent") } }
}
class A10 : B10() {
    companion object Child { init { log("A10.Child") } }
}

// child companion initializer reads from parent companion
open class B11 {
    companion object {
        val value = logValue("B11.Companion")
    }
}
class A11 : B11() {
    companion object {
        val value = B11.value + "/" + logValue("A11.Companion")
    }
}

// superinterface companions are not superclass companions
interface I12 {
    companion object { init { log("I12.Companion") } }
}
class A12 : I12 {
    companion object { init { log("A12.Companion") } }
}

// child companion object has its own superclass
open class B13 {
    companion object { init { log("B13.Companion") } }
}
open class CompanionBase13 {
    init { log("CompanionBase13.init") }
}
class A13 : B13() {
    companion object : CompanionBase13() {
        init { log("A13.Companion") }
    }
}

// two intermediate classes with no companion
open class C14 {
    companion object { init { log("C14.Companion") } }
}
open class D14 : C14()
open class B14 : D14()
class A14 : B14() {
    companion object { init { log("A14.Companion") } }
}

// superclass and superinterface both have companions
interface I15 {
    companion object {
        val marker = logValue("I15.Companion")
    }
}
open class B15 {
    companion object { init { log("B15.Companion") } }
}
class A15 : B15(), I15 {
    companion object { init { log("A15.Companion") } }
}

// generic superclass
open class B16<T> {
    companion object { init { log("B16.Companion") } }
}
class A16 : B16<String>() {
    companion object { init { log("A16.Companion") } }
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

    l = ""
    B9
    A9
    A9
    B9
    val r9 = l
    if (r9 != "B9.Companion\nA9.Companion\n") return "fail test9: '$r9'"

    l = ""
    A10
    val r10 = l
    if (r10 != "B10.Parent\nA10.Child\n") return "fail test10: '$r10'"

    l = ""
    if (A11.value != "B11.Companion/A11.Companion") return "fail test11 value: '${A11.value}'"
    val r11 = l
    if (r11 != "B11.Companion\nA11.Companion\n") return "fail test11: '$r11'"

    l = ""
    A12
    val r12 = l
    if (r12 != "A12.Companion\n") return "fail test12: '$r12'"

    l = ""
    A13
    val r13 = l
    if (r13 != "B13.Companion\nCompanionBase13.init\nA13.Companion\n") return "fail test13: '$r13'"

    l = ""
    A14
    val r14 = l
    if (r14 != "C14.Companion\nA14.Companion\n") return "fail test14: '$r14'"

    l = ""
    A15
    log("--")
    if (I15.marker != "I15.Companion") return "fail test15 marker"
    val r15 = l
    if (r15 != "B15.Companion\nA15.Companion\n--\nI15.Companion\n") return "fail test15: '$r15'"

    l = ""
    A16
    val r16 = l
    if (r16 != "B16.Companion\nA16.Companion\n") return "fail test16: '$r16'"

    return "OK"
}
