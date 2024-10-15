// RUN_PIPELINE_TILL: SOURCE
fun foo(a: Int) {
    !<!UNRESOLVED_REFERENCE!>bbb<!>
    <!UNRESOLVED_REFERENCE!>bbb<!> + a
}
