// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// DIAGNOSTICS: -UNUSED_PARAMETER

fun callAny(arg: Any?) {}
fun <T> callParam(arg: T) {}

fun testAny() {
    callAny { <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error }
    callAny l@{ <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error }
    callAny({<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error})
    callAny(({<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error}))
    callAny(l@{<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error})
    callAny((l@{<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> error}))
}

fun testAnyCall() {
    callAny {
        <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!UNRESOLVED_REFERENCE!>error<!>()
    }
}

fun testParam() {
    <!CANNOT_INFER_PARAMETER_TYPE!>callParam<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> param
    }
}

fun testParamCall() {
    <!CANNOT_INFER_PARAMETER_TYPE!>callParam<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> <!UNRESOLVED_REFERENCE!>param<!>()
    }
}

fun testNoContext() {
    { <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>it<!> -> it }
}
