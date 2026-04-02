// RUN_PIPELINE_TILL: FRONTEND
fun <T, R> baz(body: (List<R>) -> T): T = TODO()

fun test3() {
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!> {
        true
    }
    <!CANNOT_INFER_PARAMETER_TYPE!>baz<!> { x ->
        true
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeParameter */
