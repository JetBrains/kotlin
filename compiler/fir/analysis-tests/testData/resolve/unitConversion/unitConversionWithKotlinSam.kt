// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +UnitConversionsOnArbitraryExpressions
// ISSUE: KT-84393
// DIAGNOSTICS: -UNUSED_PARAMETER

fun interface KRunnable {
    fun run()
}

fun interface KSuspendRunnable {
    suspend fun run()
}

fun useKRunnable(r: KRunnable) {}

fun useKSuspendRunnable(r: KSuspendRunnable) {}

val intLambda: () -> Int = { 42 }

fun test() {
    useKRunnable(<!ARGUMENT_TYPE_MISMATCH!>intLambda<!>)
    useKSuspendRunnable(<!ARGUMENT_TYPE_MISMATCH!>intLambda<!>)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, propertyDeclaration, samConversion, suspend */
