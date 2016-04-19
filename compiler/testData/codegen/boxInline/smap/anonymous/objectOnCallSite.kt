// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}

// FILE: 2.kt

import builders.*


fun test(): String {
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

//SMAP
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
6#2:27
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt
*L
9#1:27
*E