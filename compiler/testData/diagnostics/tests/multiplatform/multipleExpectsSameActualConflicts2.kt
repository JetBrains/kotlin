// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// IGNORE_FIR_DIAGNOSTICS
// ^For `FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` with its `NoFirCompilationErrorsHandler`.

// MODULE: common

expect class A

<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun foo(it: A)

// MODULE: intermediate()()(common)

expect class D

<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun foo(it: D)

// MODULE: platform()()(intermediate)

actual typealias A = Int
actual typealias D = Int

actual <!CONFLICTING_OVERLOADS!>fun foo(it: A)<!> {}
actual <!CONFLICTING_OVERLOADS!>fun foo(it: D)<!> {}

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
