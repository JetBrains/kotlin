// RUN_PIPELINE_TILL: FRONTEND
fun foo1(): () -> String = <!REDUNDANT_RETURN!>return<!> { "some long expression "}
fun foo2(): () -> String = return<!UNRESOLVED_LABEL!>@label<!> { "some long expression "}
fun foo3(): () -> String = <!REDUNDANT_RETURN!>return<!><!SYNTAX!>@<!> { "some long expression "}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, stringLiteral */
