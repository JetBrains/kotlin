// FILE: 1.kt

package zzz

public class A(val p: Int)

operator inline fun A.iterator() = (1..p).iterator()

//SMAP ABSENT

// FILE: 2.kt

import zzz.*

fun box(): String {
    var p = 0
    for (i in A(5)) {
        p += i
    }

    return if (p == 15) "OK" else "fail: $p"
}

//SMAP
//inlineIterator.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 inlineIterator.1.kt
//InlineIterator_1Kt
//+ 2 inlineIterator.2.kt
//zzz/InlineIterator_2Kt
//*L
//1#1,24:1
//5#2:25
//*E
