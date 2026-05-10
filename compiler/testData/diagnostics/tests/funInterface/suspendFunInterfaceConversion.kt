// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -UNUSED_PARAMETER -NOTHING_TO_INLINE

fun interface SuspendRunnable {
    suspend fun invoke()
}

fun run(r: SuspendRunnable) {}

suspend fun bar() {}

fun test() {
    run(::bar)
}

/* GENERATED_FIR_TAGS: callableReference, funInterface, functionDeclaration, interfaceDeclaration, samConversion,
suspend */
