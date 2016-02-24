// FILE: 1.kt

package builders

inline fun init(init: () -> Unit) {
    init()
}

inline fun initTag2(init: () -> Unit) {
    val p = 1;
    init()
}
//{val p = initTag2(init); return p} to remove difference in linenumber processing through MethodNode and MethodVisitor should be: = initTag2(init)
inline fun head(init: () -> Unit) {val p = initTag2(init); return p}


inline fun html(init: () -> Unit) {
    return init(init)
}
//TODO SHOULD BE EMPTY
//SMAP
//smap.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 smap.2.kt
//builders/Smap_2Kt
//*L
//1#1,28:1
//*E

// FILE: 2.kt

import builders.*


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
//Smap_1Kt
//+ 2 smap.2.kt
//builders/Smap_2Kt
//*L
//1#1,38:1
//16#2:39
//4#2,9:40
//8#2,3:49
//5#2:52
//*E
