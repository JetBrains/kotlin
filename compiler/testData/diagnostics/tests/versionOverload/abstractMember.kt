// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

abstract class A {
    abstract fun <!INVALID_VERSIONING_ON_NONFINAL_FUNCTION!>foo<!>(a: Int = 0, @IntroducedAt("1") b: Int = 1)
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
integerLiteral, stringLiteral */
