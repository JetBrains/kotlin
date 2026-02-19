// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB

typealias StringProvider = () -> String

context(function: StringProvider)
fun doSomething() {
    function()
}

fun box(): String {
    with({ "" }) {
        doSomething()
    }
    return "OK"
}