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
//object.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object.1.kt
//_DefaultPackage
//+ 2 object.2.kt
//builders/BuildersPackage
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
//builders/BuildersPackage$object_2$HASH$call$1
//+ 2 object.1.kt
//_DefaultPackage$object_1$HASH
//*L
//1#1,21:1
//8#2:22
//*E