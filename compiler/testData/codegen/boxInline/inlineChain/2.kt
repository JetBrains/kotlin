class My

inline fun <T, R> T.perform(job: (T)-> R) : R {
    return job(this)
}


inline fun My.someWork(job: (String) -> Any): Unit {
    this.perform {
        job("OK")
    }
}

inline fun My.doWork (closure : (param : String) -> Unit) : Unit {
    this.someWork(closure)
}

inline fun My.doPerform (closure : (param : My) -> Int) : Int {
    return perform(closure)
}
