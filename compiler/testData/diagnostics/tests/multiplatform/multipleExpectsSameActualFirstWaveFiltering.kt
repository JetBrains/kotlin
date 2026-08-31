// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307

// MODULE: common

expect class A

expect fun foo(it: A): String

// MODULE: intermediate()()(common)

expect class B

expect fun foo(it: B): String

actual typealias A = Long

// MODULE: platform()()(intermediate)

actual typealias B = Int

actual fun foo(it: Long) = "!"
actual fun foo(it: Int) = "!"

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
