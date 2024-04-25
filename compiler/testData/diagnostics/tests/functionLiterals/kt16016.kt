// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE
val la = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!> -> }
val las = { a: Int -> }

val larg = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!> -> }(123)
val twoarg = { <!CANNOT_INFER_PARAMETER_TYPE!>a<!>, b: String, <!CANNOT_INFER_PARAMETER_TYPE!>c<!> -> }(123, "asdf", 123)
