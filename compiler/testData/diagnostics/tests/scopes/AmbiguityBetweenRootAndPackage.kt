// FILE: checkAmbiguityBetweenRootAndPackage.kt
package test

val t = <!OVERLOAD_RESOLUTION_AMBIGUITY!>testFun()<!>

// FILE: checkAmbiguityBetweenRootAndPackageRoot.kt
fun testFun() = 12

// FILE: checkAmbiguityBetweenRootAndPackageTest.kt
package test

fun testFun() = 12