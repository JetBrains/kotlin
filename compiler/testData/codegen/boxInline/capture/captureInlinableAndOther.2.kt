package test

inline fun <R> doWork(job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

