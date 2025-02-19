// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

context(a: String)
val p
    get() = context(a: String) fun (): String { return a }

fun box(): String {
    with("not OK") {
        return p("OK")
    }
}