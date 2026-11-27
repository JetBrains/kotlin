// ISSUE: KT-89592

// Test that for final methods of classes, we generate the ES6 default parameter syntax even in development mode
// (for non-final methods we should generate bridges).

// EXPECT_GENERATED_JS: expect=finalMethodNoBridge.out.js class=C1;C2 function=topLevelWithoutFileInitializerCall TARGET_BACKENDS=JS_IR_ES6

class C1(val s: Int = 2) {
    constructor(marker: Nothing?, t: Int = 19, u: Int = 20) : this(t + u)

    // A final method, thus no bridges should be generated
    fun bar(x: Int = 1) = "x=$x"
}

open class C2 {
    // A non-final method, thus a bridge should be generated
    open fun bar(x: Int = 12) = "x=$x"
}

fun topLevelWithoutFileInitializerCall(x: Int = 3) = "x=$x"

fun box(): String {
    if (C1().s != 2) return "FAIL: C1().s returned ${C1().s}"
    if (C1(null).s != 39) return "FAIL: C1(null).s returned ${C1(null).s}"
    if (C1().bar() != "x=1") return "FAIL: C1().bar() returned ${C1().bar()}"
    if (C2().bar() != "x=12") return "FAIL: C2().bar() returned ${C2().bar()}"
    if (topLevelWithoutFileInitializerCall() != "x=3") return "FAIL: topLevelWithoutFileInitializerCall() returned ${topLevelWithoutFileInitializerCall()}"
    return "OK"
}
