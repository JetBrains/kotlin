// ISSUE: KT-64069
// WITH_STDLIB

// IGNORE_LIGHT_ANALYSIS
// IGNORE_BACKEND_K1: JVM, JVM_IR, WASM
// REASON: java.lang.ClassCastException: class java.lang.String cannot be cast to class java.lang.Void (see corresponding issue)

private var enable: Boolean = true
private val string: String? by lazy {
    if (enable) {
        getT()  // No warning, but class cast exception
    } else {
        null
    }
}

fun <T> getT(): T {
    return "OK" as T
}

fun box() = string!!
