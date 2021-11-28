// FILE: 1.kt
package test

inline fun f() {}
inline fun g(x: () -> String) = x()

// FILE: 2.kt
import test.*

fun box(): String {  // KotlinDebug:
    return g {       // 2.kt:N   -> 2.kt:5
        f()          // 2.kt:N+1 -> 2.kt:6, NOT 2.kt:5
        f()          // 2.kt:N+2 -> 2.kt:7, NOT 2.kt:N+1 or 2.kt:5
        "OK"
    }
}
