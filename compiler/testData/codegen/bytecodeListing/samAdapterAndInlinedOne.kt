package test

inline fun <reified T> makeRunnable(noinline lambda: ()->Unit) : Runnable {
    return Runnable(lambda)
}

inline fun makeRunnable2(noinline lambda: ()->Unit) : Runnable {
    return Runnable(lambda)
}


fun noInline(lambda: ()->Unit) : Runnable {
    return Runnable(lambda)
}


fun noInline2(lambda: ()->Unit) : Runnable {
    return Runnable(lambda)
}