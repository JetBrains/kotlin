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