// FILE: lib.kt
inline fun bar(f: () -> String): String = f()

// FILE: main.kt
fun foo(vararg l: Long, s: String = "OK"): String =
        if (l.size == 0) s else "Fail"

fun box(): String = bar(::foo)
