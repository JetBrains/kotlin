// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType
// IGNORE_BACKEND: JS, JS_IR

fun foo(vararg l: Long, s: String = "OK"): String =
        if (l.size == 0) s else "Fail"

inline fun bar(f: () -> String): String = f()

fun box(): String = bar(::foo)
