// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> materialize(): T = TODO()

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE!>run<!> { <!CANNOT_INFER_PARAMETER_TYPE!>materialize<!>() }
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
