import builders.*
import kotlin.InlineOption.*

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
//TODO SHOULD BE LESS
//SMAP
//objectOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite.1.kt
//ObjectOnInlineCallSite_1Kt
//+ 2 objectOnInlineCallSite.2.kt
//builders/ObjectOnInlineCallSite_2Kt
//*L
//1#1,58:1
//4#2:59
//*E
//
//SMAP
//objectOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite.1.kt
//ObjectOnInlineCallSite_1Kt$test$1$1
//*L
//1#1,58:1
//*E
//
//SMAP
//objectOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite.1.kt
//ObjectOnInlineCallSite_1Kt$test$1$1
//*L
//1#1,58:1
//*E