// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

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
