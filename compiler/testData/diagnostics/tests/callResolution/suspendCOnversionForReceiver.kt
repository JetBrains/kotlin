// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-46288

fun (suspend () -> Unit).extensionFunc() {}
fun parameterFunc(func: suspend () -> Unit) {}
fun testFunc() {}

fun main() {
    parameterFunc(::testFunc)
    (::testFunc).extensionFunc()
}

/* GENERATED_FIR_TAGS: callableReference, funWithExtensionReceiver, functionDeclaration, functionalType, suspend */
