// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +TreatProvideDelegateAsConventionName
// ISSUE: KT-83538
// FILE: a.kt
package a

interface A

operator fun A.plus(other: A): A = this

// FILE: b.kt
package b

import a.<!OPERATOR_RENAMED_ON_IMPORT!>plus<!> as provideDelegate

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, interfaceDeclaration, operator, thisExpression */
