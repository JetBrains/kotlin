suspend fun <T> simpleRun(f: suspend () -> T): T = f()

suspend fun <T, R> List<T>.simpleMap(f: suspend (T) -> R): R {

}

suspend fun <T> simpleWith(t: T, f: suspend T.() -> Unit): Unit = t.f()

