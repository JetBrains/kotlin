package test

inline fun <R> doWork(job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

inline fun <R> doWork(job: ()-> R, job2: () -> R) : R {
    val k = 10;
    return notInline({k; job(); job2()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

