// FILE: fileDependencyRecursion.kt
package test

import testOther.some

val normal: Int = 1
val fromImported: Int = some

// FILE: fileDependencyRecursionOther.kt
package testOther

import test.normal

val some: Int = 1
val fromImported: Int = normal