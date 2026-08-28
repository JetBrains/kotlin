// RUN_PIPELINE_TILL: FRONTEND

@file:OptIn(ExperimentalVersionOverloading::class)

class VersionedGrid {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(
        row: Int,
        column: Int,
        @IntroducedAt("1") newValue: Int = 0,
    ) {}
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, classReference, functionDeclaration,
integerLiteral, operator, stringLiteral */
