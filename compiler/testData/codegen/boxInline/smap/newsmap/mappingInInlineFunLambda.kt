
// FILE: 1.kt

package test

inline fun myrun(s: () -> Unit) {
    val z = "myrun"
    s()
}

inline fun test(crossinline s: () -> Unit) {
    {
        val z = 1;
        myrun(s)
        val x = 1;
    }()
}

// FILE: 2.kt

import test.*

fun box(): String {
    var result = "fail"

    test {
        result = "OK"
    }

    return result
}


// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$test$1
+ 2 1.kt
test/_1Kt
*L
1#1,19:1
7#2,3:20
*E
*S KotlinDebug
*F
+ 1 1.kt
test/_1Kt$test$1
*L
14#1,3:20
*E


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
1#1,16:1
12#2,6:17
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
8#1,6:17
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$test$1
+ 2 1.kt
test/_1Kt
+ 3 2.kt
_2Kt
*L
1#1,19:1
7#2,3:20
9#3,2:23
*E
