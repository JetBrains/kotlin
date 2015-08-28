import builders.*
import kotlin.InlineOption.*

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

//SMAP
//objectOnCallSite.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 objectOnCallSite.1.kt
//ObjectOnCallSite_1Kt
//+ 2 objectOnCallSite.2.kt
//builders/ObjectOnCallSite_2Kt
//*L
//1#1,36:1
//4#2:37
//*E