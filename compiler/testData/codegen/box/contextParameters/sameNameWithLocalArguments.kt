// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

class A(var x: String) {
    fun foo(): String { return x }
}

var result = ""

context(a: A)
fun test() {
    fun local(a: A) {
        result = a.foo()
    }
    local(A("OK"))
}

fun box(): String {
    with(A("not OK")) {
        test()
    }
    return result
}