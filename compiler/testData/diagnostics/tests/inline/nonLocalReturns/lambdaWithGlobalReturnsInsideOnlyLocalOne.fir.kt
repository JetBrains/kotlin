
inline fun testSameCaptured(lambdaWithResultCaptured: () -> Unit) : String {
    doWork({lambdaWithResultCaptured()})
    return "OK"
}

inline fun <R> doWork(crossinline job: ()-> R) : R {
    return job()
}
