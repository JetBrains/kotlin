// RUN_PIPELINE_TILL: FRONTEND
package extensionFunctions

<!CONFLICTING_OVERLOADS!>fun Int.qwe(a: Float)<!> = 1

<!CONFLICTING_OVERLOADS!>fun Int.qwe(a: Float)<!> = 2

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, integerLiteral */
