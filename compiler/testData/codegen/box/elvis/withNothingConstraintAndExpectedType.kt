// ISSUE: KT-73166
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:1.9

fun <T> bar(t: T, r: Any?): T = r as T
fun foo(): String? = bar(null, "OK") ?: bar(null, "fail")

fun box(): String {
    return foo()!!
}
