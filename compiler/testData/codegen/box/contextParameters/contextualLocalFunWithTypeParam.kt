// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun <T> localWithTypeParam(b: T): T {
    context(a: T)
    fun foo(): T {
        return a
    }

    with(b) {
        return foo()
    }
}

fun box(): String {
    return localWithTypeParam("OK")
}