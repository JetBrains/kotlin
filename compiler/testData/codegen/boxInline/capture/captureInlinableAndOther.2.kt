package test

import kotlin.InlineOption.*

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

