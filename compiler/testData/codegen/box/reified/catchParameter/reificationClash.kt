// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

inline fun <reified E : Throwable> throwAndCatch(): String {
    try {
        throw AssertionError("Fail 1")
    } catch (e: E) {
        return "OK"
    } catch (e: AssertionError) {
        return "Fail 2"
    }
}

fun box(): String = throwAndCatch<AssertionError>()
