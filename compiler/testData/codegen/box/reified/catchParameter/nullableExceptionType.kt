// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

inline fun <reified E : Throwable?> throwAndCatch(): String {
    try {
        throw AssertionError("Fail")
    } catch (e: E & Any) {
        return "OK"
    }
}

fun box(): String = throwAndCatch<AssertionError?>()
