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

// FILE: 1.smap

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
1#1,12:1
5#2:13
4#2:14
4#2:15
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:13
6#1:14
7#1:15
*E
