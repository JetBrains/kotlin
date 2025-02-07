// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun test(a : String): String {

    context(a: String)
    fun foo(): String {
        return a
    }

    with("OK") {
        return foo()
    }
}

fun box(): String {
    return test("not OK")
}