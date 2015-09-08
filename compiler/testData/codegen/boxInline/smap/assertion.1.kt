import test.*

fun box(): String {
    massert(true)
    massert(true) {
        "test"
    }

    return "OK"
}

//SMAP
//assertion.1.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 assertion.1.kt
//Assertion_1Kt
//+ 2 assertion.2.kt
//test/Assertion_2Kt
//*L
//1#1,25:1
//15#2,7:26
//6#2,7:33
//*E