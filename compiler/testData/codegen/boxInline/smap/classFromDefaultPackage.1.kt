fun box(): String {
    A().foo()

    return "OK"
}

//SMAP
//classFromDefaultPackage.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 classFromDefaultPackage.1.kt
//ClassFromDefaultPackage_1Kt
//+ 2 classFromDefaultPackage.2.kt
//A
//*L
//1#1,20:1
//2#2:21
//*E
