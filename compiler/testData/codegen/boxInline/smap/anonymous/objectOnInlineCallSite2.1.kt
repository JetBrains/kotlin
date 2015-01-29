import builders.*
import kotlin.InlineOption.*

fun box(): String {
    return test()
}


//SMAP
//objectOnInlineCallSite2.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite2.1.kt
//_DefaultPackage
//+ 2 objectOnInlineCallSite2.2.kt
//builders/BuildersPackage
//*L
//1#1,32:1
//8#2,11:33
//*E
//
//SMAP
//objectOnInlineCallSite2.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnInlineCallSite2.2.kt
//builders/BuildersPackage$objectOnInlineCallSite2_2$HASH$test$1$1
//*L
//1#1,42:1
//*E