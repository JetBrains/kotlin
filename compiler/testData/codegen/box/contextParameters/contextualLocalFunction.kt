// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

fun box(): String {
    context(s: String)
    fun f() = s

    return with("OK") {
        f()
    }
}