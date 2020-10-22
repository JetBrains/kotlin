// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box(): String? {
    val log = System.getProperty("boxtest.log")
    System.clearProperty("boxtest.log") // test can be run twice
    return if (log == "bca") "OK" else log
}

val b = log("b")
val c = log("c")
val a = log("a")

fun log(message: String) {
    val value = (System.getProperty("boxtest.log") ?: "") + message
    System.setProperty("boxtest.log", value)
}