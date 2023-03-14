// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

fun run(block: () -> Unit) {}

fun branch(x: Int) = run {
    when (x) {
        1 -> TODO("1")
        2 -> TODO("2")
    }
}
