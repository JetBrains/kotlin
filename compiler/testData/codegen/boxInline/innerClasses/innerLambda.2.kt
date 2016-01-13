package test

inline fun foo1() = run {
    {
        "OK"
    }
}

var sideEffects = "fail"

inline fun foo2() = run {
    {
        Runnable {
            sideEffects = "OK"
        }
    }
}
