// FIR_IDENTICAL
// WITH_STDLIB

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

interface C {
    val test1: Int
    val test2: Int get() = 0
    var test3: Int
    var test4: Int get() = 0; set(value) {}
}
