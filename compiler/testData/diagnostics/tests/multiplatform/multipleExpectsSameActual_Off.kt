// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects -AllowMultipleExpectsForSameActual
// ISSUE: KT-69909
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS

// MODULE: common
// FILE: common.kt

expect class A
expect class B

expect <!CONFLICTING_OVERLOADS!>fun foo(it: A): String<!>
expect <!CONFLICTING_OVERLOADS!>fun foo(it: B): String<!>

fun bar(a: A, b: B) = foo(a) + foo(b)

// MODULE: platform()()(common)
// FILE: platform.kt

actual typealias A = Int
actual typealias B = Int

<!AMBIGUOUS_EXPECTS!>actual<!> fun foo(it: Int) = "!"

/* GENERATED_FIR_TAGS: additiveExpression, classDeclaration, expect, functionDeclaration */
