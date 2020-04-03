// !LANGUAGE: -CorrectSourceMappingSyntax
// FILE: 1.kt
package test

public inline fun inlineFun2(x: () -> String): String {
    return x()
}

public inline fun inlineFun(): String {
    return inlineFun2 {
        "OK"
    }
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
1#1,15:1
6#1:16
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt
*L
10#1:16
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
10#2:9
6#2,6:10
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:9
5#1,6:10
*E
