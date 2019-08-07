// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND: JS

fun foo(x: String = "O", vararg y: String): String =
        if (y.size == 0) x + "K" else "Fail"

fun call(f: () -> String): String = f()

fun box(): String {
    return call(::foo)
}
