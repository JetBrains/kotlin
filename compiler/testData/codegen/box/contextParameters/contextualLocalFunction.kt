// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun box(): String {
    context(s: String)
    fun f() = s

    return with("OK") {
        f()
    }
}