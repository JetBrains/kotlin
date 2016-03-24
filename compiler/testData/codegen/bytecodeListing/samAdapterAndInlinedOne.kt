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

// 1 final class test/_1Kt\$sam\$Runnable\$89f9321c
// 1 public final class test/_1Kt\$sam\$Runnable\$i\$89f9321c
// 2 class test/_1Kt\$sam\$