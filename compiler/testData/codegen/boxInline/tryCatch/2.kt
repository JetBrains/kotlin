class My(val value: Int)

inline fun <T, R> T.perform(job: (T)-> R) : R {
    return job(this)
}

public inline fun String.toInt2() : Int = java.lang.Integer.parseInt(this)