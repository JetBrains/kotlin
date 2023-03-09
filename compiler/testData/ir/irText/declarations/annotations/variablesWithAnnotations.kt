// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class TestAnn(val x: String)

fun foo() {
    @TestAnn("foo/testVal")
    val testVal = "testVal"

    @TestAnn("foo/testVar")
    var testVar = "testVar"
}
