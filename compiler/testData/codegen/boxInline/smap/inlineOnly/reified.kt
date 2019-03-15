// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

inline fun <reified T : Any> className() =  T::class.java.simpleName

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
6#2:13
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
6#1:13
*E