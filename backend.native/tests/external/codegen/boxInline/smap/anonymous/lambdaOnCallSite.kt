// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}
//NO_CHECK_LAMBDA_INLINING

// FILE: 2.kt

import builders.*


fun test(): String {
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
builders/_1Kt
*L
1#1,23:1
6#2:24
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
9#1:24
*E