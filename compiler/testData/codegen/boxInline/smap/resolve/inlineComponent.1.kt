import zzz.*

fun box(): String {
    var (p, l) = A(1, 11)

    return if (p == 1 && l == 11) "OK" else "fail: $p"
}

//SMAP
//inlineComponent.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 inlineComponent.1.kt
//_DefaultPackage
//+ 2 inlineComponent.2.kt
//zzz/ZzzPackage
//*L
//1#1,21:1
//5#2,3:22
//*E