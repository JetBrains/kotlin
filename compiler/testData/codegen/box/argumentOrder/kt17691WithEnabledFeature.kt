// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SPREAD_OPERATOR
// !LANGUAGE: +UseCorrectExecutionOrderForVarargArguments
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

fun foo(vararg x: Unit, y: Any) {}

fun box(): String {
    var acc1 = ""
    var acc2 = ""
    var acc3 = ""
    var acc4 = ""
    foo({ acc1 += "1" }(), y = { acc1 += "2" }())
    foo(x = *arrayOf({ acc2 += "1" }()), y = { acc2 += "2" }())
    foo(x = arrayOf({ acc3 += "1" }()), y = { acc3 += "2" }())
    foo(*arrayOf({ acc4 += "1" }()), y = { acc4 += "2" }())

    return if (acc1 == "12" && acc2 == "12" && acc3 == "12" && acc4 == "12") "OK" else "ERROR"
}
