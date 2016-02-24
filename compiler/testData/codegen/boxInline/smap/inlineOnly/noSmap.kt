// FILE: 1.kt

package test
inline fun stub() {

}

// FILE: 2.kt

fun box(): String {
    return "KO".reversed()
}

//SMAP
//noSmap.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 noSmap.1.kt
//NoSmap_1Kt
//*L
//1#1,14:1
//*E
