// FILE: main.kt
package test

import dependency.MyObject

fun usage() {
    MyObject()
}

// FILE: dependency.kt
package dependency

object MyObject {
    operator fun invoke() {}
}