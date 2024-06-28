// FILE: main.kt
package test

import dependency.MyObject

fun usage() {
    dependency.MyObject()
}

// FILE: dependency.kt
package dependency

object MyObject {
    operator fun invoke() {}
}