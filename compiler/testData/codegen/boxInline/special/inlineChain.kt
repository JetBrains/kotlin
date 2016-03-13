// FILE: 1.kt

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

// FILE: 2.kt

fun test1(): String {
    val inlineX = My()
    var d = "";
    inlineX.doWork({ z: String -> d = z; z})
    return d
}

fun test2(): Int {
    val inlineX = My()
    return inlineX.perform({ z: My -> 11})
}

fun box(): String {
    if (test1() != "OK") return "test1: ${test1()}"
    if (test2() != 11) return "test1: ${test2()}"

    return "OK"
}
