// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters +ExplicitContextArguments

fun test() {
    val func = context(s: String) fun () {}

    <!NO_VALUE_FOR_PARAMETER!>func<!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
    func(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
    <!NO_VALUE_FOR_PARAMETER!>func<!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>_<!> = "")
    <!NO_VALUE_FOR_PARAMETER!>func<!>(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>`_`<!> = "")

    (<!NO_VALUE_FOR_PARAMETER!>context(s: String) fun () {}<!>)(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>s<!> = "")
    (context(s: String) fun () {})(<!NAMED_ARGUMENTS_NOT_ALLOWED!>p1<!> = "")
    (<!NO_VALUE_FOR_PARAMETER!>context(s: String) fun () {}<!>)(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>_<!> = "")
    (<!NO_VALUE_FOR_PARAMETER!>context(s: String) fun () {}<!>)(<!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>`_`<!> = "")
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, functionDeclarationWithContext, integerLiteral,
operator, stringLiteral */
