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
+ 2 _Strings.kt
kotlin/text/StringsKt___StringsKt
*L
1#1,7:1
498#2:8
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
4#1:8
*E
