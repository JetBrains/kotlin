// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-82737


fun expectString() {
    fun foo(str: String) { }

    foo <!ARGUMENT_TYPE_MISMATCH("() -> Unit; String;  value parameter 'str' of;  '{}'")!>{}<!>
    foo <!ARGUMENT_TYPE_MISMATCH("() -> Int; String;  value parameter 'str' of;  '{ 42 }'")!>{ 42 }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String;  value parameter 'str' of;  '{ it -> 42 }'")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String;  value parameter 'str' of;  '{ it -> }'")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String;  value parameter 'str' of;  '{ it -> it }'")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }<!>
}

fun free() {
    fun bar(any: Any) { }

    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }
    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }
    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
