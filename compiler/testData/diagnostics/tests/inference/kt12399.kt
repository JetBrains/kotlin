fun foo(<!UNUSED_PARAMETER!>a<!>: Any) {
    foo({ <!CANNOT_INFER_PARAMETER_TYPE, UNUSED_ANONYMOUS_PARAMETER!>index<!> -> } {  })
}