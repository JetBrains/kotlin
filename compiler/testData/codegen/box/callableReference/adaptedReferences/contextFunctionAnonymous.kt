// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters

val a = context(x: String) fun (): String { return x }

fun foo(x: String.() -> String): String {
    return x("OK")
}

fun bar(x: context(String) () -> String): String {
    return x("OK")
}

fun baz(x: (String) -> String): String {
    return x("OK")
}

fun box(): String {
    return if ((foo(::a.get()) == "OK") && (bar(::a.get()) == "OK") && (baz(::a.get()) == "OK")) {
        "OK"
    } else "fail"
}