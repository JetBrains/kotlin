// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

fun box(): String {
    with("O") {
        return (context(a: String) fun (y: String): String = a + y)("K")
    }
}