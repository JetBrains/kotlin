// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// This is a big, ugly, semi-auto generated test.
// Use corresponding 'Small' test for debug.

fun fn0() {}
fun fn1(x0: Any) {}
fun fn2(x0: Any, x1: Any) {}
fun fn3(x0: Any, x1: Any, x2: Any) {}
fun fn4(x0: Any, x1: Any, x2: Any, x3: Any) {}
fun fn5(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any) {}
fun fn6(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any) {}
fun fn7(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any) {}
fun fn8(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any) {}
fun fn9(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any) {}
fun fn10(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any) {}
fun fn11(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any) {}
fun fn12(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any) {}
fun fn13(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any) {}
fun fn14(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any) {}
fun fn15(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any) {}
fun fn16(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any) {}
fun fn17(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any) {}
fun fn18(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any) {}
fun fn19(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any) {}
fun fn20(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any) {}
fun fn21(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any, x20: Any) {}
fun fn22(x0: Any, x1: Any, x2: Any, x3: Any, x4: Any, x5: Any, x6: Any, x7: Any, x8: Any, x9: Any, x10: Any, x11: Any, x12: Any, x13: Any, x14: Any, x15: Any, x16: Any, x17: Any, x18: Any, x19: Any, x20: Any, x21: Any) {}

val fns = arrayOf<Any>(::fn0, ::fn1, ::fn2, ::fn3, ::fn4, ::fn5, ::fn6, ::fn7, ::fn8, ::fn9,
                       ::fn10, ::fn11, ::fn12, ::fn13, ::fn14, ::fn15, ::fn16, ::fn17, ::fn18, ::fn19,
                       ::fn20, ::fn21, ::fn22)

inline fun <reified T> assertReifiedIs(x: Any, type: String) {
    val answer: Boolean
    try {
        answer = x is T
    }
    catch (e: Throwable) {
        throw AssertionError("$x is $type: should not throw exceptions, got $e")
    }
    assert(answer) { "$x is $type: failed" }
}

inline fun <reified T> assertReifiedIsNot(x: Any, type: String) {
    val answer: Boolean
    try {
        answer = x !is T
    }
    catch (e: Throwable) {
        throw AssertionError("$x !is $type: should not throw exceptions, got $e")
    }
    assert(answer) { "$x !is $type: failed" }
}

abstract class TestFnBase(val type: String) {
    abstract fun testGood(x: Any)
    abstract fun testBad(x: Any)
}

object TestFn0 : TestFnBase("Function0<*>") {
    override fun testGood(x: Any) { assertReifiedIs<Function0<*>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function0<*>>(x, type) }
}

object TestFn1 : TestFnBase("Function1<*, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function1<*, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function1<*, *>>(x, type) }
}

object TestFn2 : TestFnBase("Function2<*, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function2<*, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function2<*, *, *>>(x, type) }
}

object TestFn3 : TestFnBase("Function3<*, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function3<*, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function3<*, *, *, *>>(x, type) }
}

object TestFn4 : TestFnBase("Function4<*, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function4<*, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function4<*, *, *, *, *>>(x, type) }
}

object TestFn5 : TestFnBase("Function5<*, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function5<*, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function5<*, *, *, *, *, *>>(x, type) }
}

object TestFn6 : TestFnBase("Function6<*, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function6<*, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function6<*, *, *, *, *, *, *>>(x, type) }
}

object TestFn7 : TestFnBase("Function7<*, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function7<*, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function7<*, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn8 : TestFnBase("Function8<*, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function8<*, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function8<*, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn9 : TestFnBase("Function9<*, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function9<*, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function9<*, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn10 : TestFnBase("Function10<*, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function10<*, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function10<*, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn11 : TestFnBase("Function11<*, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function11<*, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function11<*, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn12 : TestFnBase("Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn13 : TestFnBase("Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn14 : TestFnBase("Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn15 : TestFnBase("Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn16 : TestFnBase("Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn17 : TestFnBase("Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn18 : TestFnBase("Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn19 : TestFnBase("Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn20 : TestFnBase("Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn21 : TestFnBase("Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

object TestFn22 : TestFnBase("Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>") {
    override fun testGood(x: Any) { assertReifiedIs<Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
    override fun testBad(x: Any) { assertReifiedIsNot<Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(x, type) }
}

val tests = arrayOf<TestFnBase>(TestFn0, TestFn1, TestFn2, TestFn3, TestFn4, TestFn5, TestFn6, TestFn7, TestFn8, TestFn9,
                                TestFn10, TestFn11, TestFn12, TestFn13, TestFn14, TestFn15, TestFn16, TestFn17, TestFn18, TestFn19,
                                TestFn20, TestFn21, TestFn22)

fun box(): String {
    for (fnI in 0 .. 22) {
        for (testI in 0 .. 22) {
            if (fnI == testI) {
                tests[testI].testGood(fns[fnI])
            }
            else {
                tests[testI].testBad(fns[fnI])
            }
        }
    }

    return "OK"
}
