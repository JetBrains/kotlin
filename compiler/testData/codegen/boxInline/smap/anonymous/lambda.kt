// FILE: 1.kt
// IGNORE_BACKEND: JVM_IR
package builders

inline fun call(crossinline init: () -> Unit) {
    return {
        init()
    }()
}

// FILE: 2.kt

import builders.*

//NO_CHECK_LAMBDA_INLINING
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

// FILE: 1.smap

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
builders/_1Kt$call$1
*L
1#1,11:1
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
1#1,21:1
6#2:22
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
9#1:22
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
1#1,11:1
10#2,2:12
*E
