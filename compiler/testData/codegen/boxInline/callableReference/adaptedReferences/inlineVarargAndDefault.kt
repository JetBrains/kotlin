// !LANGUAGE: +NewInference +FunctionReferenceWithDefaultValueAsOtherType

// FILE: 1.kt
package test

inline fun foo(vararg l: Long, s: String = "OK"): String =
        if (l.size == 0) s else "Fail"

inline fun bar(f: () -> String): String = f()

// FILE: 2.kt
import test.*

fun box(): String = bar(::foo)
