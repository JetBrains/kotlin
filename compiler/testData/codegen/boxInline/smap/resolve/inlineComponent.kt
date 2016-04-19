// FILE: 1.kt

package zzz

public class A(val a: Int, val b: Int)

operator inline fun A.component1() = a

operator inline fun A.component2() = b

// FILE: 2.kt

import zzz.*

fun box(): String {
    var (p, l) = A(1, 11)

    return if (p == 1 && l == 11) "OK" else "fail: $p"
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
1#1,11:1
7#2:12
9#2:13
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
6#1:12
6#1:13
*E