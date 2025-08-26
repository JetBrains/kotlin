// ISSUE: KT-68449

// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_PHASE: 2.0.0
// ^^^ KT-68449 fixed in 2.0.10

fun foo(x: Any): String {
    val result = when (x) {
        is String -> x.toString()
        is Long -> x + 10
        else -> {}
    }
    return result.toString()
}

fun box(): String {
    return foo("OK")
}
