// WITH_STDLIB
// IGNORE_BACKEND: JVM

fun <T> foo(a: Result<T>?): T? = bar(a, object : IFace<Result<T>, T> {
    override fun call(ic: Result<T>?): T? = ic?.getOrThrow()
})

interface IFace<T, R> {
    fun call(ic: T?): R?
}

fun <T, R> bar(value: T?, f: IFace<T, R>): R? {
    return f.call(value)
}

fun box(): String {
    var res = foo<Int>(Result.success(40))?.plus(2)
    if (res != 42) return "FAIL $res"
    res = foo<Int>(null)
    if (res != null) return "FAIL $res"
    return "OK"
}
