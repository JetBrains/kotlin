// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1,2.2
// ^^^ KT-80415 is fixed in 2.3.0-Beta1

fun processNumber(number: Number): Number = number

fun box(): String {
    val double: Double? = 0.0
    return if (processNumber(double ?: 0) == 0.0) "OK" else "FAIL"
}
