// IGNORE_BACKEND_K2: JVM_IR, JS_IR, NATIVE
// FIR status: ARGUMENT_TYPE_MISMATCH at ok.z(fail), actual String, expected T
// MODULE: lib
// FILE: l1.kt

val <T : CharSequence> T.z
    get() = { x: T -> this }

// FILE: l2.kt

fun test(ok: String, fail: String) = ok.z(fail)

// MODULE: main(lib)
// FILE: main.kt

fun box() = test("OK", "FAIL")