// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER
@file:OptIn(ExperimentalVersionOverloading::class)

fun inTrailing(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    block: () -> String = { x }
) = "$x/$y/${block()}"

fun inArgument(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: () -> Int = { y },
    block: () -> String
) = "$x/$y/${z()}/${block()}"

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, functionalType, lambdaLiteral,
stringLiteral */
