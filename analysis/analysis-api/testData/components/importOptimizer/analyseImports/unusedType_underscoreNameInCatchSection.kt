// FILE: main.kt
package test

import dependency.MyException

fun usage() {
    try {} catch (_: MyException) {}
}

// FILE: dependency.kt
package dependency

class MyException : RuntimeException()