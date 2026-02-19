// ISSUE: KT-83607 is fixed in 2.4.0-Beta1
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3
fun box(): String {
    val a = "a"
    val b = "b"

    if (a == "c") {
        return "FAIL1"
    } else if (a == "d") {
        return "FAIL2"
    } else if (b == "b") {
        return "OK"
    } else {
        return "FAIL3"
    }
}
