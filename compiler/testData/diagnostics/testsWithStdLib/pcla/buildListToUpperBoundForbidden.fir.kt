// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// LANGUAGE: +ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound
// ISSUE: KT-56169

fun box(): String {
    <!CANNOT_INFER_PARAMETER_TYPE!>buildList<!> {
        val foo = <!CANNOT_INFER_IT_PARAMETER_TYPE!>{ <!CANNOT_INFER_PARAMETER_TYPE!>first<!>() }<!>
        add(0, foo)
    }
    return "OK"
}

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, lambdaLiteral, localProperty, propertyDeclaration,
stringLiteral */
