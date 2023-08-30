// !DIAGNOSTICS: -UNUSED_PARAMETER

fun callAny(arg: Any?) {}
fun <T> callParam(arg: T) {}

fun testAny() {
    callAny { <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!> }
    callAny l@{ <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!> }
    callAny({<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!>})
    callAny(({<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!>}))
    callAny(l@{<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!>})
    callAny((l@{<!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>error<!>}))
}

fun testAnyCall() {
    callAny {
        <!CANNOT_INFER_PARAMETER_TYPE!>error<!> -> <!DEBUG_INFO_MISSING_UNRESOLVED!>error<!>()
    }
}

fun testParam() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>callParam<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>param<!>
    }
}

fun testParamCall() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>callParam<!> {
        <!CANNOT_INFER_PARAMETER_TYPE!>param<!> -> <!DEBUG_INFO_MISSING_UNRESOLVED!>param<!>()
    }
}

fun testNoContext() {
    { <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> }
}
