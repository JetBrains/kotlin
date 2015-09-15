package builders

inline fun call(crossinline init: () -> Unit) {
    return {
        init()
    }()
}

//SMAP
//lambda.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 lambda.2.kt
//builders/Lambda_2Kt$call$1
//*L
//1#1,18:1
//*E