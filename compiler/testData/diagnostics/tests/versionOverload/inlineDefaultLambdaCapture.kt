// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

@file:OptIn(ExperimentalVersionOverloading::class)

inline fun foo(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: Boolean = true,
    <!NON_ASCENDING_VERSION_ANNOTATION!>@IntroducedAt("1")<!> block: (String) -> String = { x.uppercase() },
) = "$x/$y/$z/${block("")}"

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, functionalType, inline,
integerLiteral, lambdaLiteral, stringLiteral */
