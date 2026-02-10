// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-71943
// WITH_STDLIB

import sun.awt.image.*

fun withCustomDecoders(originalGetDecoder: () -> ImageDecoder?) {}
fun createImage() = withCustomDecoders { null }

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType */
