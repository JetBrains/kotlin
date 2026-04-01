// LATEST_LV_DIFFERENCE
// RUN_PIPELINE_TILL: BACKEND
// RENDER_IR_DIAGNOSTICS_FULL_TEXT
// LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

@file:OptIn(ExperimentalVersionOverloading::class)

expect fun missingAnnotation(
    x: String = "O",
    @IntroducedAt("2") y: String = "foo",
): String

expect fun annotationWithWrongNumber(
    x: String = "O",
    @IntroducedAt("2") y: String = "foo",
): String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@file:OptIn(ExperimentalVersionOverloading::class)

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun missingAnnotation(
    x: String,
    y: String,
): String = x + y

<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun annotationWithWrongNumber(
    x: String,
    @IntroducedAt("3") y: String,
): String = x + y

/* GENERATED_FIR_TAGS: actual, additiveExpression, expect, functionDeclaration, stringLiteral */
