class My(val value: Int)

inline fun <T, R> T.performWithFail(job: (T)-> R, failJob : (T) -> R) : R {
    try {
        return job(this)
    } catch (e: RuntimeException) {
        return failJob(this)
    }
}

inline fun <T, R> T.performWithFail2(job: (T)-> R, failJob : (e: RuntimeException, T) -> R) : R {
    try {
        return job(this)
    } catch (e: RuntimeException) {
        return failJob(e, this)
    }
}

public inline fun String.toInt2() : Int = java.lang.Integer.parseInt(this)