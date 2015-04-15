inline fun <R> test(s: () -> R): R {
    var b = false
    try {
        return s()
    } finally {
        !b
    }
}