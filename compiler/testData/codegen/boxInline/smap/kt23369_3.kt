// FILE: + a.kt

package test

inline fun inlineFun(lambda: () -> String): String {
    return lambda()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun { "OK" }
}

// FILE: + a.smap

// FILE: 2.smap

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 + a.kt
test/__aKt
*L
1#1,8:1
6#2:9
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:9
*E