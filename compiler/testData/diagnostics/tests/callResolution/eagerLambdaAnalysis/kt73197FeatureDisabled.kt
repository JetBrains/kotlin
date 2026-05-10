// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: -EagerLambdaAnalysis
// ISSUE: KT-73197

@file:OptIn(ExperimentalTypeInference::class)
import kotlin.experimental.ExperimentalTypeInference

@OverloadResolutionByLambdaReturnType
fun submit1(x: () -> Unit) {}
@OverloadResolutionByLambdaReturnType
fun submit1(x: () -> String): String = ""

@OverloadResolutionByLambdaReturnType
fun submit2(x: () -> String): String = ""
@OverloadResolutionByLambdaReturnType
fun submit2(x: () -> Unit) {}

fun main() {
    submit1 { "" }.<!UNRESOLVED_REFERENCE!>length<!>
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>submit2<!> { "" }.<!UNRESOLVED_REFERENCE!>length<!>
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classReference, functionDeclaration, functionalType, lambdaLiteral,
stringLiteral */
