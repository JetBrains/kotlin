// FILE: 1.kt

package test

inline fun <reified T> className() =  T::class.simpleName

// FILE: 2.kt

import test.*

fun box(): String {
    val z = className<String>()
    if (z != "String") return "fail: $z"

    return "OK"
}

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
*E
