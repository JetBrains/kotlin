// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
// ISSUE: KT-82737


fun expectString() {
    fun foo(str: String) { }

    foo <!ARGUMENT_TYPE_MISMATCH("() -> Unit; String")!>{}<!>
    foo <!ARGUMENT_TYPE_MISMATCH("() -> Int; String")!>{ 42 }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }<!>
    foo <!ARGUMENT_TYPE_MISMATCH("(??? (Unknown lambda parameter type)) -> ??? (Unknown lambda return type); String")!>{ <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }<!>
}

fun free() {
    fun bar(any: Any) { }

    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> 42 }
    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> }
    bar { <!CANNOT_INFER_VALUE_PARAMETER_TYPE("it")!>it<!> -> it }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
