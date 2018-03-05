// FILE: 1.kt
// WITH_RUNTIME
package test
inline fun stub() {

}
@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
@kotlin.internal.InlineOnly
inline val prop: String
    get() = "OK"

// FILE: 2.kt
import test.*

fun box(): String {
    return prop
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
1#1,8:1
10#2:9
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
5#1:9
*E
