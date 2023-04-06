// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57777

val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
