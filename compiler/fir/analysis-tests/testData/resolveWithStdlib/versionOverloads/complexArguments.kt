// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(
    x: String,
    @IntroducedAt("1") y: Any = <!VERSION_OVERLOADS_TOO_COMPLEX_EXPRESSION!>object<!> { val n = 1 },
    @IntroducedAt("2") z: Any = <!VERSION_OVERLOADS_TOO_COMPLEX_EXPRESSION!>object<!> { val m = 1 },
): String = x

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, anonymousObjectExpression, classReference, functionDeclaration,
integerLiteral, propertyDeclaration, stringLiteral */
