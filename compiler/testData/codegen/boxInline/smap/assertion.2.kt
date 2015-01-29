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
    if (ASSERTIONS_ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

//TODO SHOUDL BE ABSENT

//SMAP
//assertion.2.kt
//Kotlin
//*S Kotlin
//*F
//+ 1 assertion.2.kt
//test/TestPackage
//*L
//1#1,34:1
//*E