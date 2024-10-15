// RUN_PIPELINE_TILL: SOURCE
fun foo(a: Any) {
    foo({ <!CANNOT_INFER_PARAMETER_TYPE!>index<!> -> } {  })
}
