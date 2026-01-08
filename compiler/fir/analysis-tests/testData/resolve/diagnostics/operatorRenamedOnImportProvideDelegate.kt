// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-83538
// FILE: a.kt
package a

interface A

operator fun A.plus(other: A): A = this

// FILE: b.kt
package b

import a.plus as provideDelegate

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, operator, thisExpression */
