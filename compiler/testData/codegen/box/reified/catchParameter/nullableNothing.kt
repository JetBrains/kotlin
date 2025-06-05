// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

// java.lang.VerifyError: Catch type is not a subclass of Throwable in exception handler 13
// IGNORE_BACKEND: ANY

inline fun <reified E : Throwable?> throwAndCatch(): String {
    try {
        throw AssertionError("Fail")
    } catch (e: E & Any) {
        return "OK"
    }
}

fun box(): String {
    return throwAndCatch<Nothing?>()
}
