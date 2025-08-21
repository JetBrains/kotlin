// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
fun a(a: String): String {
    return "local fun"
}
fun invokeOnContextType(): String {
    val a: context(String) () -> String = { "property a" }
    return a("1")
}

val b: context(String) () -> String = { "property b" }
fun invokeOnContextType2(): String {
    fun b(a: String): String {
        return "local fun"
    }
    return b("1")
}

val c: context(String) () -> String = { "property c" }
fun invokeOnContextType3(): String {
    context(a: String)
    fun c(): String {
        return "local fun"
    }
    return c("1")
}

fun box(): String {
    var result = "OK"
    if (invokeOnContextType() != "property a") result = "not OK"
    if (invokeOnContextType2() != "local fun") result = "not OK"
    if (invokeOnContextType3() != "property c") result = "not OK"
    return result
}