// !LANGUAGE: +NewInference

fun foo(x: String, vararg y: String): String =
        if (y.size == 0) x + "K" else "Fail"

fun call(f: (String) -> String, x: String): String = f(x)

fun box(): String {
    return call(::foo, "O")
}
