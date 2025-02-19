// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

class A(var x: String) {
    fun foo(): String { return x }
}

var result = ""

context(a: A)
var a: A
    get() = A("not OK")
    set(value) {
        result = a.foo()
    }

fun box(): String {
    with(A("OK")) {
        a = A("not OK")
    }
    return result
}