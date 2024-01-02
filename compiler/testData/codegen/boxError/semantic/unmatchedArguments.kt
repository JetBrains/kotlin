// IGNORE_ERRORS
// ERROR_POLICY: SEMANTIC
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// K2 issue: KT-64817

// MODULE: lib
// FILE: t.kt

var storage = ""

fun bar(a: String, b: String) { storage += a; storage += b; }

fun foo1() {
    bar("O", "K")
    bar("FAIL1")
}

fun foo2() {
    bar("FAIL2", "FAIL2", "FAIL2", "FAIL2")
}

// MODULE: main(lib)
// FILE: b.kt

fun box(): String {
    try {
        foo1()
    } catch (e: IllegalStateException) {
        try {
            foo2()
        } catch (e: IllegalStateException) {
            return storage
        }
    }
    return "FAIL"
}
