// FIR_IDENTICAL

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6

// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6


val String.test1 get() = 42

var String.test2
    get() = 42
    set(value) {}

class Host {
    val String.test3 get() = 42

    var String.test4
        get() = 42
        set(value) {}
}
