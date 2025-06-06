// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
fun foo1(): () -> String = return { "some long expression "}
fun foo2(): () -> String = return<!UNRESOLVED_LABEL!>@label<!> { "some long expression "}
fun foo3(): () -> String = return<!SYNTAX!>@<!> { "some long expression "}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
