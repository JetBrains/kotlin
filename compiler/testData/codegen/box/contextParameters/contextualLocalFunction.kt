// IGNORE_BACKEND: ANDROID
// LANGUAGE: +ContextParameters

fun box(): String {
    context(s: String)
    fun f() = s

    return with("OK") {
        f()
    }
}
