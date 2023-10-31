// FIR_IDENTICAL

inline fun testSameCaptured(lambdaWithResultCaptured: () -> Unit) : String {
    doWork({<!NON_LOCAL_RETURN_NOT_ALLOWED!>lambdaWithResultCaptured<!>()})
    return "OK"
}

inline fun <R> doWork(crossinline job: ()-> R) : R {
    return job()
}
