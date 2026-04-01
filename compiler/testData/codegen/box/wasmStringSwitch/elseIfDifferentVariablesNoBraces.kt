// ISSUE: KT-83607
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0
// ^^^ K/Wasm backend v.2.3.0 has issue KT-83607, fixed only in 2.4.0-Beta1. So, a test `current frontend + 2.3.0 backend` expectedly fails
fun box(): String {
    val a = "a"
    val b = "b"

    if (a == "b") {
        return "FAIL1"
    } else if (b == "b") {
        return "OK"
    } else if (a == "c") {
        return "FAIL2"
    } else if (b == "c") {
        return "FAIL3"
    } else {
        return "FAIL4"
    }
}