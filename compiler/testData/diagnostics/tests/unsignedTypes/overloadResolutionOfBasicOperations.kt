// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: -UNUSED_VARIABLE

fun test() {
    val x: UInt = 1u + 2u
    val y = 2u + 3u
}

/* GENERATED_FIR_TAGS: functionDeclaration, localProperty, propertyDeclaration, unsignedLiteral */
