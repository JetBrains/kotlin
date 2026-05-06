// ISSUE: KT-64069
// WITH_STDLIB
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:1.9
// ^^^ KT-64069 is fixed in 2.0.0

private var enable: Boolean = true
private val string: String? by lazy {
    if (enable) {
        getT()
    } else {
        null
    }
}

fun <T> getT(): T {
    return "OK" as T
}

fun box() = string!!
