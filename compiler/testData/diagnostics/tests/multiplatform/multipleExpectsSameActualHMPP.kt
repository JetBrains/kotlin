// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-69909, KT-88307
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// IGNORE_FIR_DIAGNOSTICS
// ^For `FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` with its `NoFirCompilationErrorsHandler`.

// MODULE: common

expect class A
expect class B
expect class C

expect <!CONFLICTING_OVERLOADS!>fun foo(it: A): String<!>
expect <!CONFLICTING_OVERLOADS!>fun foo(it: B): String<!>
expect <!CONFLICTING_OVERLOADS!>fun foo(it: C): String<!>

expect val A.<!REDECLARATION!>bar<!>: String
expect val B.<!REDECLARATION!>bar<!>: String
expect val C.<!REDECLARATION!>bar<!>: String

fun test(a: A, b: B, c: C) = foo(a) + foo(b) + foo(c) + a.bar + b.bar + c.bar

// MODULE: intermediate()()(common)

expect class D

<!UNSUPPORTED_FEATURE!><!AMBIGUOUS_EXPECTS, AMBIGUOUS_EXPECTS{METADATA}!>expect<!> fun <!EXPECT_REFINEMENT_ANNOTATION_MISSING!>foo<!>(it: D): String<!>

<!UNSUPPORTED_FEATURE!><!AMBIGUOUS_EXPECTS, AMBIGUOUS_EXPECTS{METADATA}!>expect<!> val D.<!EXPECT_REFINEMENT_ANNOTATION_MISSING!>bar<!>: String<!>

actual typealias B = D
actual typealias C = D

// MODULE: platform()()(intermediate)

actual typealias A = Int
actual typealias D = Int

actual fun foo(it: Int) = "!"

actual val Int.bar get() = "?"

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
