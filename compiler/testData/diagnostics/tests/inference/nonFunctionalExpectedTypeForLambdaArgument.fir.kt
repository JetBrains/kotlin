// !WITH_NEW_INFERENCE
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
        param -> param
    }
}

fun testParamCall() {
    callParam {
        param -> <!UNRESOLVED_REFERENCE!>param<!>()
    }
}

fun testNoContext() {
    { it -> it }
}
