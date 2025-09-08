// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE

fun main() {
    val x = <!CANNOT_INFER_PARAMETER_TYPE!>run<!> { ::<!CANNOT_INFER_PARAMETER_TYPE!>run<!> } // no error
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, lambdaLiteral, localProperty, propertyDeclaration */
