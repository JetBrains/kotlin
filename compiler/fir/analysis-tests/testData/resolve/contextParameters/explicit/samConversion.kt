// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments
// FIR_DUMP

fun interface Runnable {
    fun run()
}
fun interface StringConsumer {
    fun consume(s: String)
}
fun interface Function1<T> {
    fun compute(t: T): T
}

context(func: Runnable)
fun simple() {}

context(func: StringConsumer)
fun oneParameter() {}

fun doNothing() {}
fun acceptString(s: String) {}
fun acceptInt(i: Int) {}

context(f: Function1<T>)
fun <T> genericLambda(t: T) = f.compute(t)

fun stringIdentity(s: String) = s

fun test() {
    simple(func = {})
    simple(func = Runnable {})
    simple(func = ::doNothing)
    simple(func = Runnable(::doNothing))

    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE, CANNOT_INFER_VALUE_PARAMETER_TYPE!>it<!> -> }<!>)
    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ it: Int -> }<!>)
    simple(func = <!ARGUMENT_TYPE_MISMATCH!>{ s: String, i: Int -> }<!>)
    simple(func = ::<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>stringIdentity<!>)

    oneParameter(func = { it.length })
    oneParameter(func = StringConsumer { it.length })
    oneParameter(func = { it -> it.length })
    oneParameter(func = { it: String -> it.length })
    oneParameter(func = ::acceptString)
    oneParameter(func = StringConsumer(::acceptString))
    oneParameter(func = ::stringIdentity) // unit coercion
    oneParameter(func = StringConsumer(::stringIdentity)) // unit coercion

    oneParameter(func = <!ARGUMENT_TYPE_MISMATCH!>{ it, <!CANNOT_INFER_VALUE_PARAMETER_TYPE, CANNOT_INFER_VALUE_PARAMETER_TYPE!>x<!> -> it.length }<!>)
    oneParameter(func = <!ARGUMENT_TYPE_MISMATCH!>{ it: Int -> }<!>)
    oneParameter(func = ::<!INAPPLICABLE_CANDIDATE, INAPPLICABLE_CANDIDATE!>acceptInt<!>)

    genericLambda<String>("", f = { it.length.toString() })
    genericLambda<String>("", f = Function1 { it.length.toString() })
    genericLambda("", f = Function1<String> { it.length.toString() })
    genericLambda<String>("", f = ::stringIdentity)
    genericLambda<String>("", f = Function1(::stringIdentity))
    genericLambda("", f = Function1<String>(::stringIdentity))
    genericLambda("", f = { it.length.toString() })
    genericLambda("", f = ::stringIdentity)
}

/* GENERATED_FIR_TAGS: callableReference, funInterface, functionDeclaration, functionDeclarationWithContext,
interfaceDeclaration, lambdaLiteral, nullableType, samConversion, stringLiteral, typeParameter */
