// RUN_PIPELINE_TILL: FRONTEND

@Deprecated("", ReplaceWith("newFun()", imports = <!ARGUMENT_TYPE_MISMATCH, ASSIGNING_SINGLE_ELEMENT_TO_VARARG_IN_NAMED_FORM_FUNCTION_ERROR!>123<!>))
fun oldFun() = Unit

/* GENERATED_FIR_TAGS: functionDeclaration, integerLiteral, stringLiteral */
