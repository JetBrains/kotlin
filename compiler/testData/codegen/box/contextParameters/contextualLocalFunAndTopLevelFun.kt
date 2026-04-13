// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

fun context(a: String) { }

fun box(): String {
    context(a: String)
    fun foo(): String {
        return a
    }
    with("OK") {
        return foo()
    }
}
