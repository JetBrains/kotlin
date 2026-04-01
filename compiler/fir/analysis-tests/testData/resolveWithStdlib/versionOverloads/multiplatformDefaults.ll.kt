// LL_FIR_DIVERGENCE
// Error is not reported in {METADATA}
// LL_FIR_DIVERGENCE
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

@file:OptIn(ExperimentalVersionOverloading::class)

expect fun repeatDefault(
    x: String = "O",
    @IntroducedAt("2") y: String = "foo",
): String

expect fun missingDefault(
    x: String = "O",
    <!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("2")<!> y: String,
): String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@file:OptIn(ExperimentalVersionOverloading::class)

actual fun repeatDefault(
    x: String,
    <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>@IntroducedAt("2") y: String = "foo"<!>,
): String = x + y

actual fun missingDefault(
    x: String,
    <!ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS!>@IntroducedAt("2") y: String = "bar"<!>,
): String = x + y

/* GENERATED_FIR_TAGS: actual, additiveExpression, expect, functionDeclaration, stringLiteral */
