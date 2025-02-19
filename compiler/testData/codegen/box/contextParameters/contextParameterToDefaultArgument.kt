// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature
class C(var a: String) {
    fun foo(): String { return a }
}

context(a: C)
fun test(b: C = a): String {
    return b.foo()
}

fun box(): String {
    with(C("OK")) {
        return test()
    }
}