// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

var x = ""

context(c: String)
var foo: String
    get() = x
    set(value) {
        x = c + value
    }

fun box(): String {
    return with("O") {
        foo = "K"
        foo
    }
}
