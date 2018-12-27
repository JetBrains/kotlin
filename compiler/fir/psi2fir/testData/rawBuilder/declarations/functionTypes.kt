fun <T> simpleRun(f: (T) -> Unit): Unit = f()

fun <T, R> List<T>.simpleMap(f: (T) -> R): R {

}

fun <T> simpleWith(t: T, f: T.() -> Unit): Unit = t.f()

