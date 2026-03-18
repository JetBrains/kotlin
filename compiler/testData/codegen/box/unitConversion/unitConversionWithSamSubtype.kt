// IGNORE_BACKEND_K2: ANY
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_IR_DESERIALIZATION_TEST: JS_IR

fun interface KRunnable {
    fun run()
}

abstract class SubInt : () -> Int {
    override fun invoke(): Int {
        sideEffect += "OK"
        return 42
    }
}

fun execute(r: KRunnable) {
    r.run()
}

var sideEffect = ""

fun box(): String {
    val s = object : SubInt() {}
    execute(s)
    return sideEffect
}
