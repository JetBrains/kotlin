package builders

inline fun init(init: () -> Unit) {
    init()
}

inline fun initTag2(init: () -> Unit) {
    val p = 1;
    init()
}

inline fun head(init: () -> Unit) = initTag2(init)


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
//builders/BuildersPackage
//*L
//1#1,28:1
//*E