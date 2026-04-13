// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(<!INVALID_VERSIONING_ON_NON_OPTIONAL!>@IntroducedAt("1")<!> x: Int) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, stringLiteral */
