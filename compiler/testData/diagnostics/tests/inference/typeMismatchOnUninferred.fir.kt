// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTICS_FULL_TEXT
fun <T1> foo22(x: T1 & Any) {}

fun <T> bar(x: T & Any) {
    val z: T = x
    <!CANNOT_INFER_PARAMETER_TYPE!>foo22<!>(<!ARGUMENT_TYPE_MISMATCH!>z<!>)
}

/* GENERATED_FIR_TAGS: dnnType, functionDeclaration, localProperty, nullableType, propertyDeclaration, typeParameter */
