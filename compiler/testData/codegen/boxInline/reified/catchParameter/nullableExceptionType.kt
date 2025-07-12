// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

// FILE: lib.kt
inline fun <reified E : Throwable?> throwAndCatch(): String {
    try {
        throw AssertionError("Fail")
    } catch (e: E & Any) {
        return "OK"
    }
}

// FILE: main.kt
fun box(): String = throwAndCatch<AssertionError?>()
