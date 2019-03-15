// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JVM_IR

fun foo(x: String, y: Char = 'K'): String = x + y

fun <T, U> call(f: (T) -> U, x: T): U = f(x)

fun box(): String {
    return call(::foo, "O")
}
