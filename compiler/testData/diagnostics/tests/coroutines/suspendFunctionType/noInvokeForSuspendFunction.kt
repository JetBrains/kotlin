fun test1(sfn: suspend () -> Unit) = <!FUNCTION_EXPECTED!>sfn<!>()
fun test2(sfn: suspend () -> Unit) = sfn.<!UNRESOLVED_REFERENCE!>invoke<!>()