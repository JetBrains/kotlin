// FILE: checkAmbiguityBetweenRootAndPackage.kt
package test

val t = testFun()

// FILE: checkAmbiguityBetweenRootAndPackageRoot.kt
fun testFun() = 12

// FILE: checkAmbiguityBetweenRootAndPackageTest.kt
package test

fun testFun() = 12