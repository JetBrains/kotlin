// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun stringLambda(l: () -> String) {
    <!RETURN_VALUE_NOT_USED!>l()<!> // unused
}

fun unitLambda(l: () -> Unit) {
    l()
}

fun stringLambdaReturns(l: () -> String): String {
    return l()
}

fun main() {
    stringLambda {
        stringF() // used
    }
    unitLambda {
        <!RETURN_VALUE_NOT_USED!>stringF()<!>
    }
    <!RETURN_VALUE_NOT_USED!>stringLambdaReturns {
        stringF()
    }<!> // stringF() is used, stringLambdaReturns is unused
}
