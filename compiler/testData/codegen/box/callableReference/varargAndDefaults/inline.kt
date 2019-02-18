// !LANGUAGE: +NewInference
// IGNORE_BACKEND: JS, JVM_IR

fun foo(vararg l: Long, s: String = "OK"): String =
        if (l.size == 0) s else "Fail"

inline fun bar(f: () -> String): String = f()

fun box(): String = bar(::foo)
