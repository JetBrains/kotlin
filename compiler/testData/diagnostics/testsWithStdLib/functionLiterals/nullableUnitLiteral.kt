// RUN_PIPELINE_TILL: FRONTEND
// FULL_JDK

fun test() {
    val closeable: java.io.Closeable? = null
    val closeF = fun() { closeable?.close() }
    val closeFB = fun(): Unit = <!TYPE_MISMATCH!>closeable?.close()<!>
    val closeFR = fun() { return <!TYPE_MISMATCH!>closeable?.close()<!> }
    val closeL = { closeable?.close() }
    val closeLR = label@ { return@label closeable?.close() }
}

/* GENERATED_FIR_TAGS: anonymousFunction, functionDeclaration, javaFunction, lambdaLiteral, localProperty, nullableType,
propertyDeclaration, safeCall */
