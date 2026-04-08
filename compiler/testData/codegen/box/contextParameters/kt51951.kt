// LANGUAGE: +ContextParameters
// IGNORE_BACKEND: ANDROID
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
