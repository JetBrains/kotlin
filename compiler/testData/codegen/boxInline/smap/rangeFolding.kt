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

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt
*L
1#1,14:1
4#1:15
6#1:16
5#1:17
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt
*L
9#1:15
10#1:16
11#1:17
*E

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
test/_1Kt
*L
1#1,17:1
9#2:18
4#2,9:19
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
13#1:18
13#1,9:19
*E
