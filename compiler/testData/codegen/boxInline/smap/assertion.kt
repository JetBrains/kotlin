// FILE: 1.kt

package test

public val MASSERTIONS_ENABLED: Boolean = true

public inline fun massert(value: Boolean, lazyMessage: () -> String) {
    if (MASSERTIONS_ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}


public inline fun massert(value: Boolean, message: Any = "Assertion failed") {
    if (MASSERTIONS_ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

//SMAP ABSENT

// FILE: 2.kt

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
