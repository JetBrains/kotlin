// FILE: 1.kt
package test

inline fun inlineFun2(param: String): String {
    return param
}

inline fun inlineFun(param: String = "OK"): String {
    // The Kotlin stratum should only contain 1 out-of-range line, and
    // KotlinDebug should point it to this line:
    return inlineFun2(param)
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun()
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
5#1:15
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt
*L
11#1:15
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
1#1,8:1
8#2,4:9
5#2:13
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1,4:9
5#1:13
*E
