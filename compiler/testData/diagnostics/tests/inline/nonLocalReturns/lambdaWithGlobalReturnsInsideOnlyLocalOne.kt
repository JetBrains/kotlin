// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL

inline fun testSameCaptured(lambdaWithResultCaptured: () -> Unit) : String {
    doWork({<!NON_LOCAL_RETURN_NOT_ALLOWED!>lambdaWithResultCaptured<!>()})
    return "OK"
}

inline fun <R> doWork(crossinline job: ()-> R) : R {
    return job()
}

/* GENERATED_FIR_TAGS: crossinline, functionDeclaration, functionalType, inline, lambdaLiteral, nullableType,
stringLiteral, typeParameter */
