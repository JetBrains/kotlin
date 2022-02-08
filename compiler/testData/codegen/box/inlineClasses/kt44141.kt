// WITH_STDLIB
// KJS_FULL_RUNTIME

fun <T : Result<*>> isSuccess(a: A<T>): String =
    a.go {
        it.isSuccess
    }

class A<T> {
    fun go(f: (T) -> Boolean): String =
        if (f(Result.success(1) as T)) "OK" else "Fail"
}

fun box(): String = isSuccess(A<Result<Int>>())
