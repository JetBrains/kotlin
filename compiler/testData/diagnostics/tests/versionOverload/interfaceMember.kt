// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

interface I {
    fun foo(a: Int = 0, @IntroducedAt("1") b: Int = 1) {}
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, integerLiteral,
interfaceDeclaration, stringLiteral */
