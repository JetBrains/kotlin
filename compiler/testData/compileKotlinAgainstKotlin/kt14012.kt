// FILE: A.kt
package test

var property = "fail"
    private set

fun test() {
    property = "OK"
}

// FILE: B.kt

import test.*

fun box(): String {
    test()
    return property
}
