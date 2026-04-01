// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(@IntroducedAt("1") x: Int) {}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, stringLiteral */
