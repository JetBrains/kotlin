package test

inline fun <reified T> makeRunnable(noinline lambda: ()->Unit) : Runnable {
    return Runnable(lambda)
}
