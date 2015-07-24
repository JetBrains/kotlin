import builders.*
import kotlin.InlineOption.*

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
//TODO SHOULD BE LESS
//SMAP
//lambdaOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambdaOnInlineCallSite.1.kt
//LambdaOnInlineCallSite_1Kt
//+ 2 lambdaOnInlineCallSite.2.kt
//builders/LambdaOnInlineCallSite_2Kt
//*L
//1#1,56:1
//4#2:57
//*E
//
//SMAP
//lambdaOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambdaOnInlineCallSite.1.kt
//LambdaOnInlineCallSite_1Kt$test$1$1
//*L
//1#1,56:1
//*E
//
//SMAP
//lambdaOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambdaOnInlineCallSite.1.kt
//LambdaOnInlineCallSite_1Kt$test$1$1
//*L
//1#1,56:1
//*E