// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

var result = "Fail"

val p = object : Runnable {
    override fun run() {
        fun <T : Any> T.id() = this

        result = "OK".id()
    }
}

fun box(): String {
    p.run()
    return result
}
