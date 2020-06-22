// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// This is a big, ugly, semi-auto generated test.
// Use corresponding 'Small' test for debug.

import kotlin.test.*

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

inline fun <reified T> reifiedAsSucceeds(x: Any, operation: String) {
    x as T
}

inline fun <reified T> reifiedAsFailsWithCCE(x: Any, operation: String) {
    assertFailsWith(ClassCastException::class, "$operation should throw an exception") {
        x as T
    }
}

interface TestFnBase {
    fun testGood(x: Any)
    fun testBad(x: Any)
}

object TestFn0 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function0<*>>(
                    x, "x as Function0<*>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function0<*>>(
                    x, "x as Function0<*>")
}

object TestFn1 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function1<*, *>>(
                    x, "x as Function1<*, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function1<*, *>>(
                    x, "x as Function1<*, *>")
}

object TestFn2 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function2<*, *, *>>(
                    x, "x as Function2<*, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function2<*, *, *>>(
                    x, "x as Function2<*, *, *>")
}

object TestFn3 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function3<*, *, *, *>>(
                    x, "x as Function3<*, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function3<*, *, *, *>>(
                    x, "x as Function3<*, *, *, *>")
}

object TestFn4 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function4<*, *, *, *, *>>(
                    x, "x as Function4<*, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function4<*, *, *, *, *>>(
                    x, "x as Function4<*, *, *, *, *>")
}

object TestFn5 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function5<*, *, *, *, *, *>>(
                    x, "x as Function5<*, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function5<*, *, *, *, *, *>>(
                    x, "x as Function5<*, *, *, *, *, *>")
}

object TestFn6 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function6<*, *, *, *, *, *, *>>(
                    x, "x as Function6<*, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function6<*, *, *, *, *, *, *>>(
                    x, "x as Function6<*, *, *, *, *, *, *>")
}

object TestFn7 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function7<*, *, *, *, *, *, *, *>>(
                    x, "x as Function7<*, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function7<*, *, *, *, *, *, *, *>>(
                    x, "x as Function7<*, *, *, *, *, *, *, *>")
}

object TestFn8 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function8<*, *, *, *, *, *, *, *, *>>(
                    x, "x as Function8<*, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function8<*, *, *, *, *, *, *, *, *>>(
                    x, "x as Function8<*, *, *, *, *, *, *, *, *>")
}

object TestFn9 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function9<*, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function9<*, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function9<*, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function9<*, *, *, *, *, *, *, *, *, *>")
}

object TestFn10 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function10<*, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function10<*, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function10<*, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function10<*, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn11 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function11<*, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function11<*, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function11<*, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function11<*, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn12 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function12<*, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn13 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function13<*, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn14 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function14<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn15 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function15<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn16 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function16<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn17 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function17<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn18 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function18<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn19 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function19<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn20 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function20<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn21 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function21<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
}

object TestFn22 : TestFnBase {
    override fun testGood(x: Any) =
            reifiedAsSucceeds<Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
    override fun testBad(x: Any) =
            reifiedAsFailsWithCCE<Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>>(
                    x, "x as Function22<*, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *>")
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
