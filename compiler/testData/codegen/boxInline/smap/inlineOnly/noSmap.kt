// FILE: 1.kt
// WITH_RUNTIME
package test
inline fun stub() {

}

// FILE: 2.kt

fun box(): String {
    return "KO".reversed()
}

// FILE: 2.smap

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
*L
1#1,7:1
*E
