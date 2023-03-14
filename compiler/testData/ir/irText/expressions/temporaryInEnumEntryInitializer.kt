// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

val n: Any? = null

enum class En(val x: String?) {
    ENTRY(n?.toString())
}
