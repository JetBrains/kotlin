// RUN_PIPELINE_TILL: FRONTEND
fun foo1(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> { "some long expression "}
fun foo2(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!UNRESOLVED_LABEL!>@label<!><!> { "some long expression "}
fun foo3(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!><!SYNTAX!>@<!> { "some long expression "}
