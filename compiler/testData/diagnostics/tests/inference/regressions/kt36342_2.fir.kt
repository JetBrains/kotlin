// RUN_PIPELINE_TILL: FRONTEND
fun <K> id(arg: K): K = arg
fun <M> materialize(): M = TODO()

fun test(b: Boolean) {
    <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(if (b) {
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(<!UNRESOLVED_REFERENCE!>unresolved<!>)
    } else {
        <!CANNOT_INFER_PARAMETER_TYPE!>id<!>(
            <!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>()
        )
    })
}

/* GENERATED_FIR_TAGS: functionDeclaration, ifExpression, nullableType, typeParameter */
