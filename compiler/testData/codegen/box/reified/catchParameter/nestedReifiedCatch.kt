// LANGUAGE: +AllowReifiedTypeInCatchClause
// IGNORE_BACKEND_K1: ANY

// FILE: lib.kt
inline fun <reified E : Throwable> f(): String {
    try {
        return object {
            inline fun <reified E : Throwable> throwAndCatch(): String {
                try {
                    throw IllegalStateException("Fail 1")
                    return "Fail 2"
                } catch (e: E) {
                    return "OK"
                }
            }

            fun g(): String = throwAndCatch<IllegalStateException>()
        }.g()
    } catch (e: E) {
        return "Fail 3"
    }
}

// FILE: main.kt
fun box(): String {
    return f<AssertionError>()
}
