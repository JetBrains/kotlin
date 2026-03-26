// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP


context(func: suspend () -> String)
fun simpleSuspend() {}

fun returnsString(): String = ""

fun stringIdentity(s: String) = s

fun test() {
    val x: () -> String = { "" }
    simpleSuspend(func = { "" })
    simpleSuspend(func = ::returnsString)
    simpleSuspend(func = x)

    simpleSuspend(func = <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE, CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }<!>)
    simpleSuspend(func = <!ARGUMENT_TYPE_MISMATCH!>{ it: Int -> }<!>)
    simpleSuspend(func = <!ARGUMENT_TYPE_MISMATCH!>{ s: String, i: Int -> }<!>)
    simpleSuspend(func = ::<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>stringIdentity<!>)
}

/* GENERATED_FIR_TAGS: callableReference, funInterface, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, lambdaLiteral, nullableType, samConversion, stringLiteral, typeParameter */
