// !WITH_NEW_INFERENCE
fun foo(<!UNUSED_PARAMETER!>f<!>: String.() -> Int) {}
val test = foo(<!TYPE_MISMATCH!>fun <!NI;EXPECTED_PARAMETERS_NUMBER_MISMATCH!>()<!> = <!UNRESOLVED_REFERENCE!>length<!><!>)