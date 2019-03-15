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
        object {
            fun run () {
                res = "OK"
            }
        }.run()
    }

    return res
}


fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING

// FILE: 1.smap

// FILE: 2.smap

//7#1,3:28
//10#1,8:32 - could be merged in one big interval due preprocessing of inline function

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
1#1,26:1
7#1,3:28
10#1,8:32
6#2:27
6#2:31
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
22#1,3:28
22#1,8:32
9#1:27
22#1:31
*E

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$test$1$1
*L
1#1,26:1
*E

SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$test$1$1
*L
1#1,26:1
*E
