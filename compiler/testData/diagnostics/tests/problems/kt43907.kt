// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-43907
// WITH_STDLIB

// KT-43907: OverloadResolutionByLambdaReturnType doesn't work with suspend lambda

import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun create(a: suspend () -> Int): Int = 1

fun create(b: suspend () -> Double): Double = 1.0

val newValue = create { 3 }

/* GENERATED_FIR_TAGS: classReference, functionDeclaration, functionalType, integerLiteral, lambdaLiteral,
propertyDeclaration, suspend */
