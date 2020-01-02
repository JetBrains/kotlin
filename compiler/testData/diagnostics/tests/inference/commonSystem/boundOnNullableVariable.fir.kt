fun <T: Any> test(f: (T) -> T?) {
    doFun(f.ext())
}

fun <E : Any> Function1<E, E?>.ext(): Function0<E?> = throw Exception()
fun <R : Any> doFun(f: () -> R?) = f
