// IGNORE_BACKEND_FIR: JVM_IR
fun String?.foo() = this ?: "OK"

fun foo(i: Int?): String {
    if (i == null) return i.foo()
    return "$i"
}

fun box() = foo(null)

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: Narrowing null reference to different types inline classes