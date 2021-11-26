// WITH_STDLIB
// IGNORE_BACKEND: JVM
interface C<T> {
    abstract fun foo(x: T): String
}

open class D<T> : C<Result<T>> {
    open override fun foo(x: Result<T>): String = "???"
}

class E : D<String>() {
    override fun foo(x: Result<String>): String = x.get()
}

fun <T> Result<T>.get(): T = getOrNull()!!

fun <T> C<Result<T>>.bar(x: T) = foo(Result.success(x))

fun box() = E().bar("OK")
