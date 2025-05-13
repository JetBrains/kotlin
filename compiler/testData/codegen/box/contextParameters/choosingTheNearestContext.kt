// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

context(a: String)
fun qux(): String {
    return a
}

context(a: String)
fun test1(): String {
    context(a: String)
    fun local(): String {
        return qux()
    }

    return with("local") { local() }
}

context(b: Int, a: String)
fun test2(): String {
    context(b: Int, a: String)
    fun local(): String {
        return a + b
    }

    return with("local ") { local() }
}

fun box(): String {
    var result = "OK"
    if (with("external") { test1() } != "local") result = "not OK"
    if (with(1) { with("external") { test2() } } != "local 1") result = "not OK"
    return result
}