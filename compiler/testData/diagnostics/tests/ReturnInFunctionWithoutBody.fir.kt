fun foo1(): () -> String = return { "some long expression "}
fun foo2(): () -> String = return@label { "some long expression "}
fun foo3(): () -> String = return<!SYNTAX!>@<!> { "some long expression "}