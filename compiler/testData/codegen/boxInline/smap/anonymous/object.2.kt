package builders

inline fun call(crossinline init: () -> Unit) {
    return object {
        fun run () {
            init()
        }
    }.run()
}


//SMAP
//object.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 object.2.kt
//builders/Object_2Kt$call$1
//*L
//1#1,21:1
//*E