// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValues

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun stringLambda(l: () -> String) {
    <!RETURN_VALUE_NOT_USED!>l<!>() // unused
}

fun unitLambda(l: () -> Unit) {
    l()
}

fun stringLambdaReturns(l: () -> String): String {
    return l()
}

fun main() {
    stringLambda {
        <!RETURN_VALUE_NOT_USED!>stringF<!>() // unused because not the last statement
        stringF() // used
    }
    unitLambda {
        <!RETURN_VALUE_NOT_USED_COERCION!>stringF<!>() // unused because not the last statement
        <!RETURN_VALUE_NOT_USED_COERCION!>stringF<!>()
    }
    <!RETURN_VALUE_NOT_USED!>stringLambdaReturns<!> {
        stringF()
    } // stringF() is used, stringLambdaReturns is unused
}

/* GENERATED_FIR_TAGS: annotationUseSiteTargetFile, funWithExtensionReceiver, functionDeclaration, functionalType,
lambdaLiteral, stringLiteral */
