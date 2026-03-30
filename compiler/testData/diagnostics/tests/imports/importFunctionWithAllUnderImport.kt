// RUN_PIPELINE_TILL: BACKEND
// FILE: importFunctionWithAllUnderImport.kt
package test

import testOther.*

class B: A()
val inferTypeFromImportedFun = testFun()

// FILE: importFunctionWithAllUnderImportOther.kt
package testOther

open class A
fun testFun() = 1

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, propertyDeclaration */
