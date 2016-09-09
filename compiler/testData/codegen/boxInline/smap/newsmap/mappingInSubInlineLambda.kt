// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

inline fun test(s: () -> Unit) {
    val z = 1;
    s()
    val x = 1;
}

inline fun test2(s: () -> String): String {
    val z = 1;
    val res = s()
    return res
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"

    test {
        {
            result = test2 {
                "OK"
            }
        }()
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
1#1,20:1
6#2,4:21
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
8#1,4:21
*E

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$box$1$1
+ 2 1.kt
test/_1Kt
*L
1#1,20:1
12#2,3:21
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt$box$1$1
*L
10#1,3:21
*E