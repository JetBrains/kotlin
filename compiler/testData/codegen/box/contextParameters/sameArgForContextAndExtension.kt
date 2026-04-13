// LANGUAGE: +ContextParameters

fun foo(a: context(String) (String).() -> String): String {
    return with("OK") { a() }
}

fun box(): String {
    val y = context(p: String) fun String.() = p
    return foo(y)
}
