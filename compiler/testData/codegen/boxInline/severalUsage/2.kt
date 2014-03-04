public inline fun <R> runTest(f: () -> R): R {
    return f()
}

public inline fun <R> minByTest(f: (Int) -> R): R {
    var minValue = f(1)
    val v = f(1)
    return v
}

