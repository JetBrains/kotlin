// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

val p = { "OK" }()

val getter: String
    get() = { "OK" }()

fun f() = { "OK" }()

val obj = object : Function0<String> {
    override fun invoke() = "OK"
}

fun box(): String {
    if (p != "OK") return "FAIL"
    if (getter != "OK") return "FAIL"
    if (f() != "OK") return "FAIL"
    if (obj() != "OK") return "FAIL"

    return "OK"
}

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: IMPLEMENTING_FUNCTION_INTERFACE