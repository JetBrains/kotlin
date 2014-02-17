// FILE: root.kt
fun testFun() = 12

// FILE: otherPackage.kt
package test

fun testFun() = 12

// FILE: using.kt
import test.*

val t = <!OVERLOAD_RESOLUTION_AMBIGUITY!>testFun<!>()
