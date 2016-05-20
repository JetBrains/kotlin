// FILE: 1.kt

package test

inline fun test(s: () -> Unit) {
    val z = 1;
    s()
    val x = 1;
}


// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"
    test {
        result = "O"
    }

    test {
        result += "K"
    }

    return result
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
test/_1Kt
*L
1#1,18:1
6#2,4:19
6#2,4:23
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
7#1,4:19
11#1,4:23
*E