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
//TODO SHOULD BE LESS

//SMAP
//objectOnInlineCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite.1.kt
//_DefaultPackage
//+ 2 objectOnInlineCallSite.2.kt
//builders/BuildersPackage
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
//_DefaultPackage$objectOnInlineCallSite_1$HASH$test$1$1
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
//_DefaultPackage$objectOnInlineCallSite_1$HASH$test$1$1
//*L
//1#1,58:1
//*E