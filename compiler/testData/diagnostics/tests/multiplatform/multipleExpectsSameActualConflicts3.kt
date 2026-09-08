// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects +AllowMultipleExpectsForSingleActual
// ISSUE: KT-69909, KT-88307
// IGNORE_REVERSED_RESOLVE
// IGNORE_NON_REVERSED_RESOLVE
// IGNORE_PARTIAL_BODY_ANALYSIS
// IGNORE_FIR_DIAGNOSTICS
// ^For `FirOldFrontendMPPDiagnosticsWithLightTreeWithLatestLanguageVersionTestGenerated` with its `NoFirCompilationErrorsHandler`.

// MODULE: common

expect class C

<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun bar(it: C)

// MODULE: intermediate()()(common)

expect class D

<!AMBIGUOUS_ACTUALS{JVM}!>expect<!> fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE, EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}!>bar<!>(it: D)
actual <!CONFLICTING_OVERLOADS!>fun <!EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE{METADATA}!>bar<!>(it: C)<!> {}

actual typealias C = D

// MODULE: platform()()(intermediate)

actual typealias D = Int

actual fun bar(it: Int) {}

/* GENERATED_FIR_TAGS: actual, additiveExpression, classDeclaration, expect, functionDeclaration, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeAliasDeclaration */
