// RUN_PIPELINE_TILL: BACKEND
// FILE: root.kt
fun testFun() = 12

// FILE: otherPackage.kt
package test

fun testFun() = 12

// FILE: using.kt
package test

val t = testFun()

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, propertyDeclaration */
