// WASM_MUTE_REASON: IGNORED_IN_JS

// FILE: 1.kt
package test

inline fun foo(f: (Int, Int) -> Int): Int =
        f(42, 117)

inline fun bar (vararg xs: Int): Int {
        var sum = 0
        for (x in xs) sum += x
        return sum
}

// FILE: 2.kt
import test.*

fun box(): String = if (foo(::bar) == 159) "OK" else "fail"
