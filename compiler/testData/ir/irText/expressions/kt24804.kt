// IGNORE_BACKEND_K2: JVM_IR
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
// KT-61141: NOT_A_LOOP_LABEL: The label does not denote a loop at kt24804.kt:(256,267)
// IGNORE_BACKEND_K2: NATIVE
inline fun foo() = false

fun run(x: Boolean, y: Boolean): String {
    var z = 10
    l1@ l2@ do {
        z += 1
        if (z > 100) return "NOT_OK"
        if (x) continue@l1
        if (y) continue@l2
    } while(foo())

    return "OK"
}

fun box(): String {
    return run(true, true)
}
