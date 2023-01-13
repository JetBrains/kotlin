// See: KTIJ-23003 (ClassCastException)

// FILE: main.kt
package test

import dependency.T

fun <T> usage() {
    println(T)
}

// FILE: dependency.kt
package dependency

class T(i: Int)