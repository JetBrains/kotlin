// FILE: 1.kt

package test

inline fun <reified T> className() =  T::class.simpleName

// FILE: 2.kt

import test.*

fun box(): String {
    val z = className<String>()
    if (z != "String") return "fail: $z"

    return "OK"
}

//SMAP
//reified.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 reified.1.kt
//Reified_1Kt
//+ 2 reified.2.kt
//test/Reified_2Kt
//*L
//1#1,22:1
//3#2:23
//*E
