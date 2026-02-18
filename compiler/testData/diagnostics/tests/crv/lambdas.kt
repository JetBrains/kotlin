// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun stringLambda(l: () -> String) {
    l() // unused
}

fun unitLambda(l: () -> Unit) {
    l()
}

fun stringLambdaReturns(l: () -> String): String {
    return l()
}

fun main() {
    stringLambda {
        stringF() // unused because not the last statement
        stringF() // used
    }
    unitLambda {
        stringF() // unused because not the last statement
        stringF()
    }
    stringLambdaReturns {
        stringF()
    } // stringF() is used, stringLambdaReturns is unused
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, stringLiteral */
