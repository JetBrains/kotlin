// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE_FEATURE_TOGGLED: ReturnValueCheckerIsStable

@file:MustUseReturnValues

fun foo(): String = ""

@IgnorableReturnValue
fun bar(): Int = 42

@MustUseReturnValues
class Test {
    @IgnorableReturnValue
    fun method(): Double = 0.0
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, classDeclaration, functionDeclaration, integerLiteral, stringLiteral */
