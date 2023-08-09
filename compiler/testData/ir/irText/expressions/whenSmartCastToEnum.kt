// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

// KT-61141: absent enum fake_overrides: finalize, getDeclaringClass, clone
// IGNORE_BACKEND: NATIVE

enum class En { A, B, C }

fun test() {
    var r = ""

    val x: Any? = En.A
    if (x is En) {
        when (x) {
            En.A -> { r = "when1" }
            En.B -> {}
            En.C -> {}
        }
    }

    val y: Any = En.A
    if (y is En) {
        when (y) {
            En.A -> { r = "when2" }
            En.B -> {}
            En.C -> {}
        }
    }
}
