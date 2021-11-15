// WITH_STDLIB

fun <T> foo(a: Result<T>?): T? = bar(a) {
    it?.getOrThrow()
}

fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

fun box(): String {
    var res = foo<Int>(Result.success(40))?.plus(2)
    if (res != 42) return "FAIL $res"
    res = foo<Int>(null)
    if (res != null) return "FAIL $res"
    return "OK"
}