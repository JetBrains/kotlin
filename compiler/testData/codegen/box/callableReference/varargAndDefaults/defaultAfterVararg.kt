// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS

fun foo(vararg a: String, result: String = "OK"): String =
        if (a.size == 0) result else "Fail"

fun call(f: () -> String): String = f()

fun box(): String {
    return call(::foo)
}
