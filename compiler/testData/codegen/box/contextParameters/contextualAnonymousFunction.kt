// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

fun box(): String {
    val f = context(a: String) fun () = a

    return f("O") + with("K") {
        f()
    }
}