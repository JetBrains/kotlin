import builders.*
import kotlin.InlineOption.*

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
//object.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object.1.kt
//Object_1Kt
//+ 2 object.2.kt
//builders/Object_2Kt
//*L
//1#1,45:1
//4#2,5:46
//*E
//
//SMAP
//object.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object.2.kt
//builders/Object_2Kt$call$1
//+ 2 object.1.kt
//Object_1Kt
//*L
//1#1,21:1
//8#2:22
//*E