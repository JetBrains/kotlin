// FILE: root.kt
fun testFun() = "239"

// FILE: otherPackage.kt
package test

fun testFun() = 12

// FILE: using.kt
import test.*

val t: String = testFun()
