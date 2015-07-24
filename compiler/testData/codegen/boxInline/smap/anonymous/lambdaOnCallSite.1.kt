import builders.*
import kotlin.InlineOption.*

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