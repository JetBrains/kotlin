// FILE: 1.kt

package zzz

public class A(val p: Int)

operator inline fun A.iterator() = (1..p).iterator()

// FILE: 2.kt

import zzz.*

fun box(): String {
    var p = 0
    for (i in A(5)) {
        p += i
    }

    return if (p == 15) "OK" else "fail: $p"
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
zzz/_1Kt
*L
1#1,14:1
7#2:15
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
7#1:15
*E
