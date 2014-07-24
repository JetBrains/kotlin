import kotlin.InlineOption.ONLY_LOCAL_RETURN

inline fun testSameCaptured(lambdaWithResultCaptured: () -> Unit) : String {
    doWork({<!NON_LOCAL_RETURN_NOT_ALLOWED!>lambdaWithResultCaptured<!>()})
    return "OK"
}

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) job: ()-> R) : R {
    return job()
}