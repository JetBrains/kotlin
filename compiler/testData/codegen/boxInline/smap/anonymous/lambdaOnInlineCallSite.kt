// FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}

// FILE: 2.kt

import builders.*


inline fun test(): String {
    var res = "Fail"

    call {
        {
            res = "OK"
        }()
    }

    return res
}


fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING

// FILE: 1.smap

// FILE: 2.smap

//TODO
//7#1,3:26
//10#1,6:30 - could be merged in one big interval due preprocessing of inline function

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt
+ 2 1.kt
builders/_1Kt
*L
1#1,24:1
7#1,3:26
10#1,6:30
6#2:25
6#2:29
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
20#1,3:26
20#1,6:30
9#1:25
20#1:29
*E

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$test$1$1
*L
1#1,24:1
*E

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$test$1$1
*L
1#1,24:1
*E
