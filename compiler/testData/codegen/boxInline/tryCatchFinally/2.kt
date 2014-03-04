class My(val value: Int)

inline fun <T, R> T.performWithFinally(job: (T)-> R, finally: (T) -> R) : R {
    try {
        return job(this)
    } finally {
        return finally(this)
    }
}

inline fun <T, R> T.performWithFailFinally(job: (T)-> R, failJob : (e: RuntimeException, T) -> R, finally: (T) -> R) : R {
    try {
        return job(this)
    } catch (e: RuntimeException) {
        return failJob(e, this)
    } finally {
        return finally(this)
    }
}

inline fun String.toInt2() : Int = java.lang.Integer.parseInt(this)