// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP

context(func: () -> Unit)
fun simple() {}

context(func: (String) -> Unit)
fun oneParameter() {}

fun doNothing() {}
fun acceptString(s: String) {}
fun acceptInt(i: Int) {}

context(f: (T) -> T)
fun <T> genericLambda(t: T) = f(t)

fun stringIdentity(s: String) = s

fun test() {
    simple(func = {})
    simple(func = ::doNothing)

    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE, CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }<!>)
    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ it: Int -> }<!>)
    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ s: String, i: Int -> }<!>)
    simple(func = ::<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>stringIdentity<!>)

    oneParameter(func = { it.length })
    oneParameter(func = { it -> it.length })
    oneParameter(func = { it: String -> it.length })
    oneParameter(func = ::acceptString)
    oneParameter(func = ::stringIdentity) // unit coercion

    oneParameter(func = <!ARGUMENT_TYPE_MISMATCH!>{ it, <!CANNOT_INFER_VALUE_PARAMETER_TYPE, CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> it.length }<!>)
    oneParameter(func = <!ARGUMENT_TYPE_MISMATCH!>{ it: Int -> }<!>)
    oneParameter(func = ::<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>acceptInt<!>)

    genericLambda<String>("", f = { it.length.toString() })
    genericLambda<String>("", f = ::stringIdentity)
    genericLambda("", f = { it.length.toString() })
    genericLambda("", f = ::stringIdentity)
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionDeclarationWithContext, functionalType, lambdaLiteral */
