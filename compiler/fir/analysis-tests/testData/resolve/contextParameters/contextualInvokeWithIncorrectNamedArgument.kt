// ISSUE: KT-83524
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters

fun test(foo: context(Int) (String) -> Unit) {
    <!NO_CONTEXT_ARGUMENT!>foo<!>(<!NO_VALUE_FOR_PARAMETER!><!NAMED_ARGUMENTS_NOT_ALLOWED, NAMED_PARAMETER_NOT_FOUND!>b<!> = "")<!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, stringLiteral, typeWithContext */
