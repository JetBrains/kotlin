// RUN_PIPELINE_TILL: FRONTEND
fun test1(i: Int) = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> ->
    i
}(i)

fun test2() = <!NO_VALUE_FOR_PARAMETER!>{ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> -> i }<!>()

fun test3() = { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>i<!> -> i }(1)

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral */
