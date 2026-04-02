// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(
    @IntroducedAt("1") a: Int = <!VERSION_OVERLOADS_TOO_COMPLEX_EXPRESSION!>object<!> { val v = b }.v,
    @IntroducedAt("2") b: Int = 2,
) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, anonymousObjectExpression, classReference, functionDeclaration,
integerLiteral, propertyDeclaration, stringLiteral */
