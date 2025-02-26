// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

fun test(a : String): String {

    context(a: String)
    fun foo(): String {
        return a
    }

    with("OK") {
        return foo()
    }
}

fun box(): String {
    return test("not OK")
}