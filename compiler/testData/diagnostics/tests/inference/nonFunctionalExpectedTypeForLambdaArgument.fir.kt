// !DIAGNOSTICS: -UNUSED_PARAMETER

fun callAny(arg: Any?) {}
fun <T> callParam(arg: T) {}

fun testAny() {
    callAny { error -> error }
    callAny l@{ error -> error }
    callAny({error -> error})
    callAny(({error -> error}))
    callAny(l@{error -> error})
    callAny((l@{error -> error}))
}

fun testAnyCall() {
    callAny {
        error -> <!UNRESOLVED_REFERENCE!>error<!>()
    }
}

fun testParam() {
    callParam {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> param
    }
}

fun testParamCall() {
    callParam {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> <!UNRESOLVED_REFERENCE!>param<!>()
    }
}

fun testNoContext() {
    { <!VALUE_PARAMETER_WITH_NO_TYPE_ANNOTATION!>it<!> -> it }
}
