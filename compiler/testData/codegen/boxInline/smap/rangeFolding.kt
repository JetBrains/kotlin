// FILE: 1.kt
package test

inline fun f() {}
inline fun g() {}
inline fun h() {}

inline fun together() {
    f() // new range 1.kt:N -> 1.kt:4
    h() // new range 1.kt:N+1 -> 1.kt:6 because of different call site
    g() // new range 1.kt:N+2 -> 1.kt:5 for the same reason
}

// FILE: 2.kt
import test.*

fun box(): String {
    // 1. new range 2.kt:N -> 1.kt:9
    // 2. new range 2.kt:N+1 -> 1.kt:4
    // 3. extend to 2.kt:N+1..N+7 -> 1.kt:4..10 and use N+7 for 1.kt:10
    // 4. use N+2 for 1.kt:5
    // 5. extend to 2.kt:N+1..N+8 -> 1.kt:4..11 and use N+8 for 1.kt:11
    // 6. use N+3 for 1.kt:6
    // 7. extend to 2.kt:N+1..N+9 -> 1.kt:4..12 and use N+9 for 1.kt:12
    // steps 4 and 6 *should not* create new ranges
    together()
    return "OK"
}
