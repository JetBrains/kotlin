// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=intoIfBranch.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun write(): Int {
    js("globalThis.x = 5")
    return 5
}
fun doSomething(x: Int) = js("globalThis.y = x")
fun read(): Int = js("globalThis.x")
fun neverTrue(): Boolean = js("!!false[0]")

fun box(): String {
    val tmp = write()
    if (neverTrue()) {
        val tmp2 = tmp
        doSomething(tmp2)
    }
    if (read() != 5) return "write was eliminated"
    return "OK"
}
