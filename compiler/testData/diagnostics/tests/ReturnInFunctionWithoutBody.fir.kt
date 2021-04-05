fun foo1(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!> { "some long expression "}
fun foo2(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY, RETURN_NOT_ALLOWED!>return@label<!> { "some long expression "}
fun foo3(): () -> String = <!RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY!>return<!><!SYNTAX!>@<!> { "some long expression "}
