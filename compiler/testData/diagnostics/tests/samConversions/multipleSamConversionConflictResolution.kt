// RUN_PIPELINE_TILL: BACKEND
fun interface Runnable {
    fun run()
}

fun foo(r: Runnable, f: Runnable) = 1
fun foo(r: Runnable, f: () -> Unit) = ""

fun test(): String {
    return foo(Runnable {}, {})
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, stringLiteral */
