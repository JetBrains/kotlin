// FILE: importFunctionWithAllUnderImport.kt
package test

import testOther.*

class B: A()
val inferTypeFromImportedFun = testFun()

// FILE: importFunctionWithAllUnderImportOther.kt
package testOther

open class A
fun testFun() = 1
