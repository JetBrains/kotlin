// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

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
