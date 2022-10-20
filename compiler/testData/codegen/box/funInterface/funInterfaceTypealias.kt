fun interface KRunnable {
    fun invoke()
}

fun interface KConsumer<T> {
    fun accept(value: T)
}

typealias KRunnableAlias = KRunnable

typealias StringConsumer = KConsumer<String>

fun foo(f: KRunnable) = f

fun <T> bar(f: KConsumer<T>) = f

fun box(): String {
    var result = ""
    foo(KRunnable {
        bar(KConsumer<String> {
            result += it
        }).accept("O")
    }).invoke()
    foo(KRunnableAlias {
        bar(StringConsumer {
            result += it
        }).accept("K")
    }).invoke()
    return result
}
