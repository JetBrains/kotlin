fun interface FunIFace<T, R> {
    fun call(ic: T): R
}

fun <T, R> bar(value: T, f: FunIFace<T, R>): R {
    return f.call(value)
}

class X(val value: Any)

fun <T> gfn(a: X): T =
    bar(a) {
        it.value as T
    }

fun box() =
    gfn<String>(X("OK"))