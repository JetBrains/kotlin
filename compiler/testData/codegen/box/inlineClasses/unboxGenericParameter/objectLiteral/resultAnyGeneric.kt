// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// IGNORE_BACKEND: JVM
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class IC<T: Any>(val value: T)

fun <T: Any> foo(a: Result<T>, ic: IC<T>): Pair<T, Any> = bar(a, ic, object : IFace<Result<T>, IC<T>, Pair<T, Any>> {
    override fun call(a: Result<T>, ic: IC<T>): Pair<T, Any> = a.getOrThrow() to ic.value
})

interface IFace<T1, T2, R> {
    fun call(t1: T1, t2: T2): R
}

fun <T1, T2, R> bar(t1: T1, t2: T2, f: IFace<T1, T2, R>): R {
    return f.call(t1, t2)
}

fun Pair<Any, Any>.join(): String = "$first$second"

fun box(): String = foo<Any>(Result.success("O"), IC("K")).join()