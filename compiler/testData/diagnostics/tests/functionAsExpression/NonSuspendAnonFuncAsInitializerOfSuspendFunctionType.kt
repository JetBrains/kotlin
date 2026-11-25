// RUN_PIPELINE_TILL: FRONTEND
fun test () {
    val local: suspend () -> Unit <!INITIALIZER_TYPE_MISMATCH("suspend () -> Unit; () -> Unit")!>=<!> fun () {};
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, functionalType, localProperty, propertyDeclaration,
suspend */
