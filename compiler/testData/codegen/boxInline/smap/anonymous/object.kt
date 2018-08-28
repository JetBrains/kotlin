// FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
package builders

inline fun call(crossinline init: () -> Unit) {
    return object {
        fun run () {
            init()
        }
    }.run()
}

// FILE: 2.kt

import builders.*


fun test(): String {
    var res = "Fail"

    call {
        res = "OK"
    }

    return res
}


fun box(): String {
    return test()
}
//NO_CHECK_LAMBDA_INLINING

// FILE: 1.smap

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
builders/_1Kt$call$1
*L
1#1,13:1
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
builders/_1Kt
*L
1#1,22:1
6#2,5:23
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
9#1,5:23
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
builders/_1Kt$call$1
+ 2 2.kt
_2Kt
*L
1#1,13:1
10#2,2:14
*E
