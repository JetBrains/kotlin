// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-65235
// LANGUAGE: +NewShapeForFirstLastFunctionsInKotlinList

fun somewhere(x: List<String>) {
    val x: String = x.first()

}
