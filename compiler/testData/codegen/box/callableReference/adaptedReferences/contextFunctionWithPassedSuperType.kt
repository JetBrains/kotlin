// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

fun foo(a: context(String) () -> String): String {
    return a("OK")
}

fun CharSequence.test1(): String {
    return this.toString()
}

fun test2(a: CharSequence): String {
    return a.toString()
}

fun box(): String {
    if ((foo(CharSequence::test1) == "OK") && (foo(::test2) == "OK")) return "OK"
    return "NOK"
}