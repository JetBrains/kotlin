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


//SMAP
//lambda.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.1.kt
//_DefaultPackage
//+ 2 lambda.2.kt
//builders/BuildersPackage
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
//builders/BuildersPackage$lambda_2$HASH$call$1
//+ 2 lambda.1.kt
//_DefaultPackage$lambda_1$HASH
//*L
//1#1,18:1
//8#2:19
//*E