// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// DONT_TARGET_EXACT_BACKEND: WASM

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
