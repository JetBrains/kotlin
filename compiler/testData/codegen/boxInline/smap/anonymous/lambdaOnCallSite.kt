// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    return init()
}
//NO_CHECK_LAMBDA_INLINING
//SMAP ABSENT

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


//SMAP
//lambdaOnCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambdaOnCallSite.1.kt
//LambdaOnCallSite_1Kt
//+ 2 lambdaOnCallSite.2.kt
//builders/LambdaOnCallSite_2Kt
//*L
//1#1,34:1
//4#2:35
//*E
