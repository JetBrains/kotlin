// LANGUAGE: -CorrectSourceMappingSyntax
// FILE: 1.kt
package test

public inline fun inlineFun2(x: () -> String): String {
    return x()
}

public inline fun inlineFun(): String {
    return inlineFun2 {
        "OK"
    }
}

// FILE: 2.kt
import test.*

fun box(): String {
    return inlineFun()
}
