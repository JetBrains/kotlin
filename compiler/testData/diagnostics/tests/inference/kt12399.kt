// RUN_PIPELINE_TILL: FRONTEND
fun foo(a: Any) {
    foo({ <!CANNOT_INFER_PARAMETER_TYPE!>index<!> -> } {  })
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral */
