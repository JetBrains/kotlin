// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE
// Related issue: KT-28654

fun <K> select(): K = run <!ARGUMENT_TYPE_MISMATCH!>{ }<!>

fun test() {
    val x: Int = select()
    val t = <!CANNOT_INFER_PARAMETER_TYPE!>select<!>()
}

/* GENERATED_FIR_TAGS: functionDeclaration, lambdaLiteral, localProperty, nullableType, propertyDeclaration,
typeParameter */
