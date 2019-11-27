// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR

inline fun String.app(f: (String) -> String) = f(this)

fun fff(s: String, n: Int = 42) = s

fun box() = "OK".app(::fff)
