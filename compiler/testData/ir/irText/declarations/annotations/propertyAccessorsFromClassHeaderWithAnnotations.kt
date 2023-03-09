// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

annotation class A(val x: String)

class C(
    @get:A("C.x.get") val x: Int,
    @get:A("C.y.get") @set:A("C.y.set") var y: Int
)
