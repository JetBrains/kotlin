// SKIP_KT_DUMP
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

data class A(val x: Int) {
    val String.x: String get() = this
}
