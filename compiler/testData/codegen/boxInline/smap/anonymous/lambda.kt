// FILE: 1.kt

package builders

inline fun call(crossinline init: () -> Unit) {
    return {
        init()
    }()
}

//SMAP
//lambda.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.2.kt
//builders/Lambda_2Kt$call$1
//*L
//1#1,18:1
//*E

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

//SMAP
//lambda.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.1.kt
//Lambda_1Kt
//+ 2 lambda.2.kt
//builders/Lambda_2Kt
//*L
//1#1,46:1
//4#2:47
//*E
//
//SMAP
//lambda.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.2.kt
//builders/Lambda_2Kt$call$1
//+ 2 lambda.1.kt
//Lambda_1Kt
//*L
//1#1,18:1
//8#2,2:19
//*E
