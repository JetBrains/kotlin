fun foo(a: Any) {
    foo({ <!VALUE_PARAMETER_WITHOUT_EXPLICIT_TYPE!>index<!> -> } {  })
}