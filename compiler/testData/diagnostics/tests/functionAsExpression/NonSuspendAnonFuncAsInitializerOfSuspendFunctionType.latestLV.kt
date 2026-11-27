// RUN_PIPELINE_TILL: FRONTEND
fun test () {
    val local: suspend () -> Unit <!INITIALIZER_TYPE_MISMATCH!>=<!> fun () {};
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, localProperty, propertyDeclaration,
suspend */
