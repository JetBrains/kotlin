package test

import kotlin.InlineOption.*

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) job: ()-> R, inlineOptions(ONLY_LOCAL_RETURN) job2: () -> R) : R {
    val k = 10;
    return notInline({k; job(); job2()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

