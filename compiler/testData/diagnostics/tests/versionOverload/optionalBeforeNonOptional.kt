// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(@IntroducedAt("1") b: Int = 1, <!INVALID_VERSIONING_ON_REQUIRED_AFTER_OPTIONAL!>x: String<!>) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral, stringLiteral */
