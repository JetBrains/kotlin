// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307

// MODULE: common

expect class A
expect class B
expect class C

expect fun foo(it: A): String
expect fun foo(it: B): String
expect fun foo(it: C): String

expect val A.bar: String
expect val B.bar: String
expect val C.bar: String

fun test(a: A, b: B, c: C) = foo(a) + foo(b) + foo(c) + a.bar + b.bar + c.bar

// MODULE: intermediate()()(common)

expect class D

expect fun foo(it: D): String

expect val D.bar: String

actual typealias B = D
actual typealias C = D

// MODULE: platform()()(intermediate)

actual typealias A = Int
actual typealias D = Int

actual fun foo(it: Int) = "!"

actual val Int.bar get() = "?"

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
