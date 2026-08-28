// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// IGNORE_FIR_DIAGNOSTICS
// ^For `FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` with its `NoFirCompilationErrorsHandler`.

// MODULE: common

expect class A
expect class B

expect fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE!>foo<!>(it: A)
<!CONFLICTING_OVERLOADS!>fun foo(it: B)<!> {}

// MODULE: platform()()(common)

actual typealias A = Int
actual typealias B = Int

actual fun foo(it: Int) {}

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
