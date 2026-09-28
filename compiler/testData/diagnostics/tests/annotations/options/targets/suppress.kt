// RUN_PIPELINE_TILL: CODEGEN
@file:Suppress("abc")

fun foo(): Int {
    @Suppress("xyz") return 1
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, functionDeclaration, integerLiteral, stringLiteral */
