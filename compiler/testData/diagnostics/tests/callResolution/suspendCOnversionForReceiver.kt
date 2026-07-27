// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-46288

fun (suspend () -> Unit).extensionFunc() {}
fun parameterFunc(func: suspend () -> Unit) {}
fun testFunc() {}

fun main() {
    parameterFunc(::testFunc)
    (::testFunc).<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>extensionFunc<!>()
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType, suspend */
