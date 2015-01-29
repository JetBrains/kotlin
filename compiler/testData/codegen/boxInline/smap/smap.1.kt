import builders.*
import kotlin.InlineOption.*

inline fun test(): String {
    var res = "Fail"

    html {
        head {
            res = "OK"
        }
    }

    return res
}


fun box(): String {
    var expected = test();

    return expected
}

//SMAP
//smap.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 smap.1.kt
//_DefaultPackage
//+ 2 smap.2.kt
//builders/BuildersPackage
//*L
//1#1,38:1
//16#2:39
//4#2,9:40
//8#2,3:49
//5#2:52
//*E