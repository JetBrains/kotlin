// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

class Ref(var x: Int)

fun test1() {
    var x = 0
    x = 1
    x = x + 1
}

fun test2(r: Ref) {
    r.x = 0
}
