// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

fun <T> localWithTypeParam(b: T): T {
    context(a: T)
    fun foo(): T {
        return a
    }

    with(b) {
        return foo()
    }
}

fun box(): String {
    return localWithTypeParam("OK")
}