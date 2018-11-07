// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR

fun foo(x: String, y: String = "K"): String = x + y

fun call(f: (String) -> String, x: String): String = f(x)

fun box(): String {
    return call(::foo, "O")
}
