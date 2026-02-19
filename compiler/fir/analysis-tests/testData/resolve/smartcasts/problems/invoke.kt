// RUN_PIPELINE_TILL: BACKEND
fun Any.withInvoke(f: String.() -> Unit) {
    if (this is String) {
        f() // Should be OK
    }
}

fun String.withInvoke(f: String.() -> Unit) {
    f()
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, ifExpression, isExpression,
smartcast, thisExpression, typeWithExtension */
