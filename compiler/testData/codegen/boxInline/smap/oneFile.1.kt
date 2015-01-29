fun box(): String {
    return test {
        "K"
    }
}

inline fun test(p: () -> String): String {
    var pd = ""
    pd = "O"
    return pd + p()
}
//TODO should be empty
//SMAP
//oneFile.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 oneFile.1.kt
//_DefaultPackage
//*L
//1#1,22:1
//*E