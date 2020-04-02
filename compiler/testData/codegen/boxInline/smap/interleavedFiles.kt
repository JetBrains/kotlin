// FILE: 1.kt
package test

inline fun f() {}

// FILE: 2.kt
import test.*

inline fun g() {}

inline fun h() {
    f() // line N+1 -> 1.kt:4
    g() // line N+2 -> 2.kt:4
    f() // line N+3 -> 1.kt:4 again
}

fun box(): String {
    h()
    return "OK"
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
1#1,17:1
4#1:19
7#1:21
8#1:23
4#1,7:24
4#2:18
4#2:20
4#2:22
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
8#1:19
13#1:21
13#1:23
13#1,7:24
7#1:18
9#1:20
13#1:22
*E
