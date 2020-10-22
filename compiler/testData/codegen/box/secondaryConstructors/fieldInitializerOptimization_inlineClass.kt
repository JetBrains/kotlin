// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

open class Base {
    open fun setup() {}
    init { setup() }
}

inline class Z(val y: Int)

class Derived : Base {
    constructor() : super()
    override fun setup() {
        x = Z(1)
    }

    // Technically, this field initializer comes after the superclass
    // constructor is called. However, we optimize away field initializers
    // which set fields to their default value, which is why x ends up with
    // value 1 after the constructor call.
    var x = Z(0)
}

fun box(): String {
    val d = Derived()
    if (d.x.y != 1) return "fail"
    return "OK"
}
